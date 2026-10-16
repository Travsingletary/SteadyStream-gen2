import http from 'http';
import assert from 'assert';

function testEndpoint(path, method = 'GET', body = null) {
  return new Promise((resolve, reject) => {
    const req = http.request({
      hostname: '127.0.0.1',
      port: 43128,
      path,
      method,
      headers: {
        'Content-Type': 'application/json'
      }
    }, res => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => resolve({ status: res.statusCode, body: data }));
    });
    req.on('error', reject);
    if (body) req.write(typeof body === 'string' ? body : JSON.stringify(body));
    req.end();
  });
}

async function runTests() {
  console.log("Testing Mock Xtream API /player_api.php...");
  const auth = await testEndpoint('/player_api.php?username=travis&password=123');
  assert.strictEqual(auth.status, 200);
  const authData = JSON.parse(auth.body);
  assert.strictEqual(authData.user_info.username, "travis_singletary");

  console.log("Testing Channel List /api/channels...");
  const chs = await testEndpoint('/api/channels');
  assert.strictEqual(chs.status, 200);
  const chList = JSON.parse(chs.body);
  assert.ok(chList.length >= 4);

  console.log("Testing AI Assist /api/ai-assist...");
  const ai = await testEndpoint('/api/ai-assist', 'POST', { query: "find me the game" });
  assert.strictEqual(ai.status, 200);
  const aiData = JSON.parse(ai.body);
  assert.ok(aiData.reply.includes("sports") || aiData.reply.includes("Sports"));
  assert.ok(aiData.matchedChannels.length > 0);

  console.log("Testing Config /api/config...");
  const cfg = await testEndpoint('/api/config');
  assert.strictEqual(cfg.status, 200);
  const cfgData = JSON.parse(cfg.body);
  assert.strictEqual(cfgData.sportsPicksEnabled, false);
  assert.strictEqual(cfgData.communityEnabled, true);

  console.log("All Web Companion tests passed!");
}

runTests().catch(err => {
  console.error("Test failed:", err);
  process.exit(1);
});
