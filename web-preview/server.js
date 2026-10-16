import http from 'http';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PORT = 43128;

// In-memory mock data
let channels = [
  {
    id: "skyras_sports_1",
    channelNumber: 101,
    name: "SKYRAS Sports 1 HD",
    streamUrl: "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
    logoUrl: "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=128&auto=format&fit=crop&q=80",
    categoryName: "Sports",
    epgChannelId: "skyras.sports1",
    isFavorite: true,
    nowPlaying: "NBA Championship Replay: Lakers vs Celtics",
    nowTime: "18:00 - 20:30",
    nowProgress: 65,
    nextProgram: "Sports Center Live: Post-Game Analysis",
    nextTime: "20:30 - 21:30",
    type: "LIVE"
  },
  {
    id: "skyras_news_24",
    channelNumber: 102,
    name: "SKYRAS 24/7 Global News",
    streamUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
    logoUrl: "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=128&auto=format&fit=crop&q=80",
    categoryName: "News",
    epgChannelId: "skyras.news24",
    isFavorite: false,
    nowPlaying: "Global Markets & Tech Wire Special",
    nowTime: "19:00 - 20:00",
    nowProgress: 40,
    nextProgram: "Prime Evening World News Report",
    nextTime: "20:00 - 21:00",
    type: "LIVE"
  },
  {
    id: "skyras_cinema_1",
    channelNumber: 103,
    name: "SKYRAS Premiere Cinema",
    streamUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
    logoUrl: "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=128&auto=format&fit=crop&q=80",
    categoryName: "Movies",
    epgChannelId: "skyras.cinema1",
    isFavorite: true,
    nowPlaying: "Original Cinema: Sci-Fi Odyssey (2026)",
    nowTime: "18:30 - 21:00",
    nowProgress: 52,
    nextProgram: "Action Showcase: Midnight Pursuit",
    nextTime: "21:00 - 23:15",
    type: "LIVE"
  },
  {
    id: "skyras_vod_doc",
    channelNumber: 104,
    name: "VOD: Autonomous Systems",
    streamUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
    logoUrl: "https://images.unsplash.com/photo-1518770660439-4636190af475?w=128&auto=format&fit=crop&q=80",
    categoryName: "Documentary",
    epgChannelId: null,
    isFavorite: false,
    nowPlaying: "Feature: Autonomous Neural Networks",
    nowTime: "On Demand",
    nowProgress: 15,
    nextProgram: "Behind the Tech: Ep. 2",
    nextTime: "Next Up",
    type: "VOD"
  }
];

let appConfig = {
  sportsPicksEnabled: false,
  communityEnabled: true,
  communityUrl: "https://discord.gg/skyras",
  llmProvider: "mock", // or 'openai_compatible'
  openAiBaseUrl: "https://api.openai.com/v1",
  openAiModel: "gpt-4o-mini"
};

// M3U Parser helper
function parseM3uText(text) {
  const lines = text.split(/\r?\n/);
  const parsed = [];
  let curExt = null;
  const attrRegex = /([a-zA-Z0-9_\-]+)="([^"]*)"/g;

  for (let line of lines) {
    line = line.trim();
    if (line.startsWith('#EXTINF:')) {
      const comma = line.lastIndexOf(',');
      const attrs = comma !== -1 ? line.substring(8, comma) : line.substring(8);
      const name = comma !== -1 ? line.substring(comma + 1).trim() : 'Channel';
      
      const meta = { name };
      let match;
      while ((match = attrRegex.exec(attrs)) !== null) {
        meta[match[1].toLowerCase()] = match[2];
      }
      curExt = meta;
    } else if (line.length > 0 && !line.startsWith('#')) {
      parsed.push({
        id: curExt?.['tvg-id'] || `ch_${parsed.length + 1}`,
        channelNumber: parsed.length + 101,
        name: curExt?.name || curExt?.['tvg-name'] || `Channel ${parsed.length + 1}`,
        streamUrl: line,
        logoUrl: curExt?.['tvg-logo'] || null,
        categoryName: curExt?.['group-title'] || 'General',
        isFavorite: false,
        nowPlaying: "Live Broadcast",
        nowTime: "Live",
        nowProgress: 50,
        nextProgram: "Upcoming Program",
        nextTime: "Next",
        type: line.endsWith('.mp4') ? "VOD" : "LIVE"
      });
      curExt = null;
    }
  }
  return parsed;
}

// Local mock Xtream API handler
function handleMockXtream(urlObj, res) {
  const action = urlObj.searchParams.get('action');
  res.writeHead(200, { 'Content-Type': 'application/json' });

  if (!action) {
    // Auth info
    res.end(JSON.stringify({
      user_info: {
        username: "travis_singletary",
        status: "Active",
        exp_date: "1792195200",
        is_trial: "0",
        active_cons: "1",
        max_connections: "5"
      },
      server_info: {
        url: "localhost",
        port: PORT.toString(),
        server_protocol: "http",
        timezone: "America/New_York"
      }
    }));
    return;
  }

  if (action === 'get_live_categories') {
    res.end(JSON.stringify([
      { category_id: "1", category_name: "SKYRAS Sports", parent_id: 0 },
      { category_id: "2", category_name: "SKYRAS Movies", parent_id: 0 }
    ]));
    return;
  }

  if (action === 'get_live_streams') {
    res.end(JSON.stringify([
      {
        num: 1,
        name: "SKYRAS Sports 1 (Xtream)",
        stream_type: "live",
        stream_id: 101,
        stream_icon: "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=128",
        epg_channel_id: "sports1",
        category_id: "1"
      }
    ]));
    return;
  }

  res.end(JSON.stringify([]));
}

// AI Assist engine handler
function handleAiAssist(body) {
  const q = (body.query || '').toLowerCase();
  let reply = "Here are channels from your SKYRAS TV lineup matching your request.";
  let matched = [];

  if (q.includes('game') || q.includes('nba') || q.includes('sports') || q.includes('lakers')) {
    reply = "I found live sports coverage right now! Tune into SKYRAS Sports 1 HD for the NBA championship replay.";
    matched = channels.filter(c => c.categoryName.toLowerCase() === 'sports');
  } else if (q.includes('what') || q.includes('now') || q.includes('guide')) {
    reply = "Here is what's on right now across your favorite live channels.";
    matched = channels.slice(0, 3);
  } else if (q.includes('movie') || q.includes('vod') || q.includes('film')) {
    reply = "Looking for entertainment? You have SKYRAS Premiere Cinema and on-demand titles ready to stream.";
    matched = channels.filter(c => c.categoryName.toLowerCase().includes('movie') || c.type === 'VOD');
  } else {
    matched = channels.filter(c => c.name.toLowerCase().includes(q) || c.categoryName.toLowerCase().includes(q));
  }

  return {
    reply,
    matchedChannels: matched,
    suggestedActions: [
      "Watch SKYRAS Sports 1 HD",
      "Show all Sports",
      "What's on right now?"
    ]
  };
}

const server = http.createServer((req, res) => {
  const urlObj = new URL(req.url, `http://${req.headers.host}`);

  // CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  // API endpoints
  if (urlObj.pathname === '/player_api.php') {
    handleMockXtream(urlObj, res);
    return;
  }

  if (urlObj.pathname === '/api/channels' && req.method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(channels));
    return;
  }

  if (urlObj.pathname === '/api/channels/favorite' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      const data = JSON.parse(body || '{}');
      channels = channels.map(c => c.id === data.channelId ? { ...c, isFavorite: !c.isFavorite } : c);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true, channels }));
    });
    return;
  }

  if (urlObj.pathname === '/api/config' && req.method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(appConfig));
    return;
  }

  if (urlObj.pathname === '/api/config' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      const data = JSON.parse(body || '{}');
      appConfig = { ...appConfig, ...data };
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(appConfig));
    });
    return;
  }

  if (urlObj.pathname === '/api/import/m3u' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      const parsed = parseM3uText(body);
      if (parsed.length > 0) {
        channels = parsed;
      }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true, count: parsed.length, channels }));
    });
    return;
  }

  if (urlObj.pathname === '/api/ai-assist' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      const data = JSON.parse(body || '{}');
      const response = handleAiAssist(data);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(response));
    });
    return;
  }

  // Serve static UI
  if (urlObj.pathname === '/' || urlObj.pathname === '/index.html') {
    const htmlPath = path.join(__dirname, 'index.html');
    fs.readFile(htmlPath, (err, data) => {
      if (err) {
        res.writeHead(500);
        res.end("Error loading UI");
        return;
      }
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(data);
    });
    return;
  }

  res.writeHead(404);
  res.end("Not Found");
});

server.listen(PORT, '127.0.0.1', () => {
  console.log(`SKYRAS TV companion running at http://127.0.0.1:${PORT}`);
});
