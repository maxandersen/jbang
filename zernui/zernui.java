///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javalin:javalin:6.3.0
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.0
//DEPS org.slf4j:slf4j-simple:2.0.16
//JAVA 17+
//DESCRIPTION ZernUI – local web UI for publishing stories via the Zernio API

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import io.javalin.*;
import io.javalin.http.*;
import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

public class zernui {

    static final Path CONFIG = Path.of(System.getProperty("user.home"), ".config", "zernui", "config.json");
    static final String ZERNIO = "https://api.zernio.com";
    static final ObjectMapper JSON = new ObjectMapper();
    static final HttpClient HTTP = HttpClient.newHttpClient();

    // ── entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        // suppress verbose Jetty/Javalin startup chatter
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");

        int port = args.length > 0 ? Integer.parseInt(args[0]) : 7777;

        var app = Javalin.create();

        app.get("/",                ctx -> ctx.html(UI));
        app.get("/api/status",      zernui::apiStatus);
        app.post("/api/key",        zernui::apiSaveKey);
        app.delete("/api/key",      zernui::apiDeleteKey);
        app.get("/api/profiles",    ctx -> proxyGET(ctx, "/v1/profiles"));
        app.get("/api/accounts",    ctx -> proxyGET(ctx, "/v1/accounts"));
        app.get("/api/posts",       ctx -> proxyGET(ctx, "/v1/posts"));
        app.post("/api/posts",      ctx -> proxyPOST(ctx, "/v1/posts"));

        app.start("localhost", port);

        String url = "http://localhost:" + port;
        System.out.println("ZernUI → " + url);
        openBrowser(url);
    }

    // ── config helpers ────────────────────────────────────────────────────────

    static String readKey() {
        try { return JSON.readTree(CONFIG.toFile()).path("key").asText(null); }
        catch (Exception e) { return null; }
    }

    static void writeKey(String key) throws Exception {
        Files.createDirectories(CONFIG.getParent());
        JSON.writerWithDefaultPrettyPrinter()
            .writeValue(CONFIG.toFile(), JSON.createObjectNode().put("key", key));
        try {
            Files.setPosixFilePermissions(CONFIG,
                Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException ignored) {} // Windows
    }

    // ── API handlers ──────────────────────────────────────────────────────────

    static void apiStatus(Context ctx) throws Exception {
        ctx.json(JSON.createObjectNode().put("configured", readKey() != null).toString());
    }

    static void apiSaveKey(Context ctx) throws Exception {
        String key = JSON.readTree(ctx.body()).path("key").asText("");
        if (!key.startsWith("sk_") || key.length() < 10) {
            ctx.status(400).result("{\"error\":\"Key must start with sk_\"}");
            return;
        }
        // validate key against Zernio
        var resp = zernioRequest("GET", key, "/v1/profiles", null);
        if (resp.statusCode() == 401 || resp.statusCode() == 403) {
            ctx.status(401).result("{\"error\":\"Invalid API key – Zernio rejected it\"}");
            return;
        }
        writeKey(key);
        ctx.result("{\"ok\":true}");
    }

    static void apiDeleteKey(Context ctx) throws Exception {
        Files.deleteIfExists(CONFIG);
        ctx.result("{\"ok\":true}");
    }

    // ── proxy helpers ─────────────────────────────────────────────────────────

    static void proxyGET(Context ctx, String path) throws Exception {
        String key = readKey();
        if (key == null) { ctx.status(401).result("{\"error\":\"Not configured\"}"); return; }
        String q = ctx.queryString();
        var resp = zernioRequest("GET", key, path + (q != null ? "?" + q : ""), null);
        ctx.status(resp.statusCode()).contentType("application/json").result(resp.body());
    }

    static void proxyPOST(Context ctx, String path) throws Exception {
        String key = readKey();
        if (key == null) { ctx.status(401).result("{\"error\":\"Not configured\"}"); return; }
        var resp = zernioRequest("POST", key, path, ctx.body());
        ctx.status(resp.statusCode()).contentType("application/json").result(resp.body());
    }

    static HttpResponse<String> zernioRequest(String method, String key, String path, String body)
            throws Exception {
        var builder = HttpRequest.newBuilder()
            .uri(URI.create(ZERNIO + path))
            .header("Authorization", "Bearer " + key)
            .header("Accept", "application/json");
        if ("POST".equals(method) && body != null)
            builder.header("Content-Type", "application/json")
                   .POST(HttpRequest.BodyPublishers.ofString(body));
        else
            builder.GET();
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                Desktop.getDesktop().browse(new URI(url));
            else
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
        } catch (Exception ignored) {}
    }

    // ── embedded UI ───────────────────────────────────────────────────────────

    static final String UI = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>ZernUI</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:#f1f5f9;color:#1e293b;min-height:100vh}
.screen{display:none;min-height:100vh}
.screen.active{display:flex}

/* ── Setup ── */
#setup{align-items:center;justify-content:center}
.card{background:#fff;border-radius:16px;padding:48px;width:420px;box-shadow:0 4px 24px rgba(0,0,0,.08)}
.logo{font-size:28px;font-weight:800;color:#4f46e5;letter-spacing:-1px;margin-bottom:4px}
.logo em{color:#818cf8;font-style:normal}
.tagline{font-size:13px;color:#64748b;margin-bottom:32px}
.card h2{font-size:18px;font-weight:700;margin-bottom:6px}
.card>p{font-size:13px;color:#64748b;margin-bottom:24px}
label{display:block;font-size:12px;font-weight:600;color:#374151;margin-bottom:5px;text-transform:uppercase;letter-spacing:.4px}
input[type=password],input[type=text],textarea{
  width:100%;padding:10px 14px;border:1.5px solid #e2e8f0;border-radius:8px;
  font-size:14px;outline:none;transition:border-color .15s;background:#fff}
input[type=password]:focus,input[type=text]:focus,textarea:focus{border-color:#4f46e5}
.btn{display:inline-flex;align-items:center;gap:6px;padding:9px 18px;border-radius:8px;font-size:13px;font-weight:600;border:none;cursor:pointer;transition:all .15s}
.btn-primary{background:#4f46e5;color:#fff;justify-content:center;width:100%;padding:12px;margin-top:14px}
.btn-primary:hover{background:#4338ca}
.btn-primary:disabled{background:#a5b4fc;cursor:not-allowed}
.btn-ghost{background:transparent;color:#64748b;font-size:12px}
.btn-ghost:hover{background:#f1f5f9;color:#1e293b}
.btn-danger{background:#fef2f2;color:#ef4444;font-size:12px}
.btn-danger:hover{background:#fee2e2}
.note{background:#f0fdf4;border:1px solid #bbf7d0;border-radius:8px;padding:10px 12px;font-size:12px;color:#15803d;margin-top:14px;line-height:1.5}
.err{background:#fef2f2;border:1px solid #fecaca;border-radius:8px;padding:10px 12px;font-size:13px;color:#dc2626;margin-top:10px;display:none}

/* ── App shell ── */
#app{flex-direction:column}
.hdr{background:#fff;border-bottom:1px solid #e2e8f0;padding:0 24px;height:54px;display:flex;align-items:center;justify-content:space-between;flex-shrink:0}
.hdr .logo{font-size:18px;margin-bottom:0}
.hdr-btns{display:flex;gap:8px}
.main{display:grid;grid-template-columns:1fr 290px;gap:16px;padding:20px 24px;max-width:1080px;width:100%;margin:0 auto}

/* ── Compose ── */
.panel{background:#fff;border-radius:12px;padding:20px;box-shadow:0 1px 4px rgba(0,0,0,.06)}
.panel h3{font-size:14px;font-weight:700;color:#374151;margin-bottom:14px;text-transform:uppercase;letter-spacing:.4px}
textarea.story{min-height:180px;resize:vertical;border:1.5px solid #e2e8f0;border-radius:8px;padding:12px;font-size:15px;line-height:1.65;font-family:inherit}
.char-row{display:flex;flex-wrap:wrap;gap:6px;margin-top:8px}
.cbadge{font-size:11px;padding:3px 8px;border-radius:20px;background:#f1f5f9;color:#64748b;font-weight:600}
.cbadge.warn{background:#fff7ed;color:#ea580c}
.cbadge.over{background:#fef2f2;color:#dc2626}
.sched-row{display:flex;align-items:center;gap:10px;margin-top:14px;flex-wrap:wrap}
.sched-row label{margin:0;text-transform:none;letter-spacing:0;font-size:14px;font-weight:400;cursor:pointer}
input[type=datetime-local]{padding:8px 10px;border:1.5px solid #e2e8f0;border-radius:7px;font-size:13px;font-family:inherit}
.pub-btn{background:#4f46e5;color:#fff;padding:11px 22px;border-radius:9px;border:none;cursor:pointer;font-size:14px;font-weight:700;display:flex;align-items:center;gap:8px;margin-top:16px;transition:background .15s}
.pub-btn:hover{background:#4338ca}
.pub-btn:disabled{background:#a5b4fc;cursor:not-allowed}

/* ── Accounts ── */
.acc-group{margin-bottom:14px}
.grp-hdr{font-size:11px;font-weight:700;color:#94a3b8;text-transform:uppercase;letter-spacing:.5px;display:flex;align-items:center;justify-content:space-between;margin-bottom:6px}
.all-lnk{font-size:11px;font-weight:600;color:#4f46e5;cursor:pointer;text-transform:none;letter-spacing:0}
.all-lnk:hover{text-decoration:underline}
.acc-item{display:flex;align-items:center;gap:9px;padding:7px 8px;border-radius:8px;cursor:pointer;transition:background .1s}
.acc-item:hover{background:#f8fafc}
.acc-item input[type=checkbox]{width:15px;height:15px;cursor:pointer;accent-color:#4f46e5;flex-shrink:0}
.pbadge{width:26px;height:26px;border-radius:6px;display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:800;color:#fff;flex-shrink:0}
.acc-info{flex:1;min-width:0}
.acc-handle{font-size:13px;font-weight:500;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.acc-plat{font-size:11px;color:#94a3b8}

/* ── Posts ── */
.post-item{padding:10px 0;border-bottom:1px solid #f1f5f9}
.post-item:last-child{border-bottom:none}
.post-body{font-size:13px;color:#374151;white-space:pre-wrap;line-height:1.5;margin-bottom:5px}
.post-meta{font-size:11px;color:#94a3b8;display:flex;align-items:center;gap:6px;flex-wrap:wrap}
.mini-badge{font-size:10px;padding:2px 5px;border-radius:4px;font-weight:700;color:#fff}
.empty{text-align:center;padding:28px;color:#94a3b8;font-size:13px;line-height:1.6}

/* ── Loading ── */
.loading{display:flex;align-items:center;justify-content:center;padding:32px;color:#94a3b8;font-size:13px}
.spin{width:18px;height:18px;border:2px solid #e2e8f0;border-top-color:#4f46e5;border-radius:50%;animation:spin .55s linear infinite;margin-right:9px;flex-shrink:0}
@keyframes spin{to{transform:rotate(360deg)}}

/* ── Toast ── */
.toast{position:fixed;bottom:22px;right:22px;background:#1e293b;color:#fff;padding:11px 18px;border-radius:10px;font-size:13px;font-weight:500;box-shadow:0 8px 24px rgba(0,0,0,.2);transform:translateY(70px);opacity:0;transition:all .28s;z-index:999}
.toast.show{transform:translateY(0);opacity:1}
.toast.ok{background:#16a34a}
.toast.err{background:#dc2626}

@media(max-width:680px){.main{grid-template-columns:1fr}}
</style>
</head>
<body>

<!-- Setup -->
<div id="setup" class="screen">
  <div class="card">
    <div class="logo">Zern<em>UI</em></div>
    <div class="tagline">Simpler publishing for Zernio</div>
    <h2>Connect your Zernio account</h2>
    <p>Enter your Zernio API key to start publishing across all your connected platforms from one simple interface.</p>
    <label for="k">API Key</label>
    <input type="password" id="k" placeholder="sk_…" autocomplete="off" onkeydown="if(event.key==='Enter')connectKey()">
    <div id="setup-err" class="err"></div>
    <button class="btn btn-primary" id="conn-btn" onclick="connectKey()">Connect</button>
    <div class="note">🔒 Your key is stored only in <code>~/.config/zernui/config.json</code> on your machine. It is never sent anywhere except the Zernio API.</div>
  </div>
</div>

<!-- App -->
<div id="app" class="screen">
  <div class="hdr">
    <div class="logo">Zern<em style="color:#818cf8;font-style:normal">UI</em></div>
    <div class="hdr-btns">
      <button class="btn btn-danger" onclick="disconnect()">Disconnect</button>
    </div>
  </div>

  <div class="main">
    <div style="display:flex;flex-direction:column;gap:16px">

      <!-- Compose -->
      <div class="panel">
        <h3>Compose Story</h3>
        <textarea class="story" id="story" placeholder="What's happening? Write your story here…" oninput="updateCounts()"></textarea>
        <div class="char-row" id="char-row"></div>
        <div class="sched-row">
          <input type="checkbox" id="sched-tog" onchange="toggleSched()">
          <label for="sched-tog">Schedule for later</label>
          <input type="datetime-local" id="sched-time" style="display:none">
        </div>
        <button class="pub-btn" id="pub-btn" onclick="publish()">
          <svg width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" viewBox="0 0 24 24"><path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/></svg>
          Publish
        </button>
      </div>

      <!-- Recent posts -->
      <div class="panel">
        <h3>Recent Posts</h3>
        <div id="posts"><div class="loading"><div class="spin"></div>Loading…</div></div>
      </div>
    </div>

    <!-- Accounts -->
    <div class="panel" style="align-self:start;position:sticky;top:20px">
      <h3>Publish to</h3>
      <div id="accounts"><div class="loading"><div class="spin"></div>Loading…</div></div>
    </div>
  </div>
</div>

<div class="toast" id="toast"></div>

<script>
const PLAT = {
  twitter:         {label:'X / Twitter', color:'#000',   abbr:'X',  limit:280},
  x:               {label:'X / Twitter', color:'#000',   abbr:'X',  limit:280},
  facebook:        {label:'Facebook',    color:'#1877f2', abbr:'Fb', limit:63206},
  instagram:       {label:'Instagram',   color:'#c13584', abbr:'Ig', limit:2200},
  linkedin:        {label:'LinkedIn',    color:'#0a66c2', abbr:'Li', limit:3000},
  tiktok:          {label:'TikTok',      color:'#010101', abbr:'Tt', limit:2200},
  youtube:         {label:'YouTube',     color:'#ff0000', abbr:'Yt', limit:5000},
  threads:         {label:'Threads',     color:'#000',   abbr:'Th', limit:500},
  bluesky:         {label:'Bluesky',     color:'#0085ff', abbr:'Bs', limit:300},
  pinterest:       {label:'Pinterest',   color:'#e60023', abbr:'Pi', limit:500},
  reddit:          {label:'Reddit',      color:'#ff4500', abbr:'Re', limit:40000},
  discord:         {label:'Discord',     color:'#5865f2', abbr:'Dc', limit:2000},
  telegram:        {label:'Telegram',    color:'#26a5e4', abbr:'Tg', limit:4096},
  snapchat:        {label:'Snapchat',    color:'#f7c948', abbr:'Sn', limit:250, tc:'#000'},
  whatsapp:        {label:'WhatsApp',    color:'#25d366', abbr:'Wa', limit:65536},
  google_business: {label:'Google Biz',  color:'#4285f4', abbr:'Gb', limit:1500},
};

function pi(platform) {
  const k = (platform||'').toLowerCase().replace(/[\s-]/g,'_');
  return PLAT[k] || {label:platform||'Unknown', color:'#64748b', abbr:(platform||'?').slice(0,2).toUpperCase(), limit:5000};
}

let accounts = [];
let selected = new Set();

// ── Init ──────────────────────────────────────────────────────────────────────
async function init() {
  const r = await fetch('/api/status').then(r=>r.json()).catch(()=>({}));
  r.configured ? showApp() : show('setup');
}

function show(id) {
  document.querySelectorAll('.screen').forEach(s=>s.classList.remove('active'));
  document.getElementById(id).classList.add('active');
}

function showApp() {
  show('app');
  loadAccounts();
  loadPosts();
}

// ── Setup ─────────────────────────────────────────────────────────────────────
async function connectKey() {
  const key = document.getElementById('k').value.trim();
  const err = document.getElementById('setup-err');
  const btn = document.getElementById('conn-btn');
  err.style.display = 'none';

  if (!key.startsWith('sk_')) {
    err.textContent = 'API key must start with sk_';
    err.style.display = 'block';
    return;
  }

  btn.disabled = true;
  btn.textContent = 'Connecting…';

  try {
    const res = await fetch('/api/key', {
      method:'POST', headers:{'Content-Type':'application/json'},
      body:JSON.stringify({key})
    });
    if (res.ok) { showApp(); }
    else {
      const d = await res.json();
      err.textContent = d.error || 'Failed to connect. Check your API key.';
      err.style.display = 'block';
    }
  } catch {
    err.textContent = 'Could not reach ZernUI server.';
    err.style.display = 'block';
  }

  btn.disabled = false;
  btn.textContent = 'Connect';
}

async function disconnect() {
  if (!confirm('Remove your Zernio API key from ZernUI?')) return;
  await fetch('/api/key', {method:'DELETE'});
  accounts = [];
  selected.clear();
  document.getElementById('k').value = '';
  show('setup');
}

// ── Accounts ──────────────────────────────────────────────────────────────────
async function loadAccounts() {
  document.getElementById('accounts').innerHTML = '<div class="loading"><div class="spin"></div>Loading…</div>';
  try {
    const [pr, ar] = await Promise.all([fetch('/api/profiles'), fetch('/api/accounts')]);
    const pdata = pr.ok ? await pr.json() : {};
    const adata = ar.ok ? await ar.json() : {};
    accounts = adata.data || adata.accounts || (Array.isArray(adata) ? adata : []);
    const profiles = pdata.data || pdata.profiles || (Array.isArray(pdata) ? pdata : []);
    renderAccounts(profiles, accounts);
  } catch {
    document.getElementById('accounts').innerHTML = '<div class="empty">Failed to load accounts</div>';
  }
}

function renderAccounts(profiles, accs) {
  const el = document.getElementById('accounts');
  if (!accs.length) {
    el.innerHTML = '<div class="empty">No accounts found.<br>Connect them in your Zernio dashboard first.</div>';
    return;
  }

  // build profile name map
  const pmap = {};
  profiles.forEach(p => pmap[p.id] = p.name || p.id);

  // group accounts by profile
  const groups = {};
  accs.forEach(a => {
    const pid = a.profile_id || a.profileId || 'default';
    (groups[pid] = groups[pid] || []).push(a);
  });

  let html = '';
  for (const [pid, grp] of Object.entries(groups)) {
    const pname = pmap[pid] || (pid === 'default' ? 'Accounts' : pid);
    html += `<div class="acc-group">
      <div class="grp-hdr">${esc(pname)}<span class="all-lnk" onclick="toggleGroup('${pid}',event)">all</span></div>`;
    grp.forEach(a => {
      const id = a.id || a._id || a.account_id;
      const info = pi(a.platform || a.type || a.service);
      html += `<div class="acc-item" onclick="toggleAcc('${id}',event)">
        <input type="checkbox" id="a-${id}" value="${id}" onchange="onCB('${id}')">
        <div class="pbadge" style="background:${info.color};color:${info.tc||'#fff'}">${info.abbr}</div>
        <div class="acc-info">
          <div class="acc-handle">${esc(a.username||a.handle||a.name||'Account')}</div>
          <div class="acc-plat">${info.label}</div>
        </div>
      </div>`;
    });
    html += '</div>';
  }
  document.getElementById('accounts').innerHTML = html;
}

function toggleAcc(id, e) {
  if (e.target.type==='checkbox') return;
  const cb = document.getElementById('a-'+id);
  if (cb) { cb.checked = !cb.checked; onCB(id); }
}

function onCB(id) {
  const cb = document.getElementById('a-'+id);
  cb?.checked ? selected.add(id) : selected.delete(id);
  updateCounts();
}

function toggleGroup(pid, e) {
  e.stopPropagation();
  const grp = accounts.filter(a=>(a.profile_id||a.profileId||'default')===pid);
  const allOn = grp.every(a=>selected.has(a.id||a._id||a.account_id));
  grp.forEach(a=>{
    const id = a.id||a._id||a.account_id;
    const cb = document.getElementById('a-'+id);
    if (cb) cb.checked = !allOn;
    allOn ? selected.delete(id) : selected.add(id);
  });
  updateCounts();
}

// ── Compose ───────────────────────────────────────────────────────────────────
function updateCounts() {
  const len = document.getElementById('story').value.length;
  const sel = accounts.filter(a=>selected.has(a.id||a._id||a.account_id));
  if (!sel.length) {
    document.getElementById('char-row').innerHTML = `<span class="cbadge">${len} chars</span>`;
    return;
  }
  const seen = new Set();
  let html = '';
  sel.forEach(a=>{
    const k = (a.platform||a.type||'').toLowerCase();
    if (seen.has(k)) return; seen.add(k);
    const info = pi(k);
    const cls = len>info.limit?'over':len>info.limit*.88?'warn':'';
    html += `<span class="cbadge ${cls}">${info.abbr} ${len}/${info.limit}</span>`;
  });
  document.getElementById('char-row').innerHTML = html || `<span class="cbadge">${len} chars</span>`;
}

function toggleSched() {
  document.getElementById('sched-time').style.display =
    document.getElementById('sched-tog').checked ? 'block' : 'none';
}

async function publish() {
  const text = document.getElementById('story').value.trim();
  if (!text)          { toast('Write something first!',          'err'); return; }
  if (!selected.size) { toast('Select at least one account',     'err'); return; }

  const btn = document.getElementById('pub-btn');
  btn.disabled = true;
  btn.innerHTML = '<div class="spin" style="border-top-color:#fff;width:14px;height:14px"></div>Publishing…';

  const schedVal = document.getElementById('sched-tog').checked
    ? document.getElementById('sched-time').value : null;

  const payload = {
    content: text,
    account_ids: [...selected],
    ...(schedVal ? {scheduled_at: new Date(schedVal).toISOString()} : {})
  };

  try {
    const res = await fetch('/api/posts', {
      method:'POST', headers:{'Content-Type':'application/json'},
      body:JSON.stringify(payload)
    });
    const data = await res.json().catch(()=>({}));
    if (res.ok) {
      toast(schedVal ? 'Story scheduled!' : 'Story published!', 'ok');
      document.getElementById('story').value = '';
      selected.clear();
      document.querySelectorAll('.acc-item input[type=checkbox]').forEach(cb=>cb.checked=false);
      updateCounts();
      setTimeout(loadPosts, 900);
    } else {
      toast(data.error || data.message || 'Publish failed', 'err');
    }
  } catch { toast('Network error', 'err'); }

  btn.disabled = false;
  btn.innerHTML = '<svg width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" viewBox="0 0 24 24"><path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/></svg> Publish';
}

// ── Posts ─────────────────────────────────────────────────────────────────────
async function loadPosts() {
  const el = document.getElementById('posts');
  try {
    const res = await fetch('/api/posts?limit=10');
    const data = await res.json().catch(()=>({}));
    const posts = data.data || data.posts || (Array.isArray(data) ? data : []);
    if (!posts.length) {
      el.innerHTML = '<div class="empty">No posts yet.<br>Write your first story above!</div>';
      return;
    }
    el.innerHTML = posts.slice(0,10).map(p=>{
      const body = p.content||p.text||p.body||'';
      const date = p.created_at||p.createdAt||p.published_at;
      const accs  = p.accounts||p.platforms||[];
      const badges = accs.map(a=>{
        const info = pi(a.platform||a.type||a);
        return `<span class="mini-badge" style="background:${info.color};color:${info.tc||'#fff'}">${info.abbr}</span>`;
      }).join('');
      const dateStr = date ? new Date(date).toLocaleDateString(undefined,{month:'short',day:'numeric',hour:'2-digit',minute:'2-digit'}) : '';
      return `<div class="post-item">
        <div class="post-body">${esc(body.slice(0,220))}${body.length>220?'…':''}</div>
        <div class="post-meta">${badges}${dateStr?`<span>${dateStr}</span>`:''}</div>
      </div>`;
    }).join('');
  } catch {
    el.innerHTML = '<div class="empty">Could not load posts</div>';
  }
}

// ── Utilities ─────────────────────────────────────────────────────────────────
function toast(msg, type='') {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.className = 'toast '+type;
  setTimeout(()=>t.classList.add('show'), 10);
  setTimeout(()=>t.classList.remove('show'), 3200);
}

function esc(s) {
  return (s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

init();
</script>
</body>
</html>
""";
}
