import os
import io
import time
import uuid
import json
import hashlib
import random
import asyncio
import hmac
import re
import ipaddress
from collections import defaultdict
from typing import Dict, List, Optional, Set, Any, Iterable, Union
from datetime import datetime, timezone
import urllib.request
import urllib.parse
import socket
import subprocess

from fastapi import FastAPI, WebSocket, WebSocketDisconnect, Request, Response, UploadFile, File, Form, HTTPException
from fastapi.responses import HTMLResponse, JSONResponse, StreamingResponse, FileResponse, Response as RawResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from fastapi.middleware.cors import CORSMiddleware
from starlette.middleware.gzip import GZipMiddleware
from starlette.datastructures import Headers
from starlette.responses import PlainTextResponse
from PIL import Image, ImageOps
try:
    import qrcode
except ImportError:
    qrcode = None

import config
import database
import network_intel
import animal_avatars
import tunnel
import ai_admin

app = FastAPI(title="AnonChat", docs_url=None, redoc_url=None, openapi_url=None)

def extract_host(host_header: str) -> str:
    host_header = (host_header or "").strip()
    if host_header.startswith("["):
        end_bracket = host_header.find("]")
        if end_bracket != -1:
            return host_header[:end_bracket + 1]
    return host_header.split(":")[0]

class StrictHostValidationMiddleware:
    def __init__(self, app, allowed_hosts: Iterable[str]):
        self.app = app
        self.allowed_hosts = list(allowed_hosts)

    async def __call__(self, scope, receive, send):
        if scope["type"] not in ("http", "websocket"):
            await self.app(scope, receive, send)
            return

        headers = Headers(scope=scope)
        raw_host = headers.get("host", "").strip()
        if not raw_host:
            res = PlainTextResponse("Missing host header", status_code=400)
            await res(scope, receive, send)
            return

        host = extract_host(raw_host).lower()
        is_valid = False
        for pattern in self.allowed_hosts:
            p_lower = pattern.lower()
            if p_lower == "*":
                is_valid = True
                break
            if host == p_lower:
                is_valid = True
                break
            if p_lower.startswith("*.") and host.endswith(p_lower[1:]):
                is_valid = True
                break

        if is_valid:
            await self.app(scope, receive, send)
        else:
            res = PlainTextResponse("Invalid host header", status_code=400)
            await res(scope, receive, send)

# Strict Host validation allowlist: legitimate production domains, local dev, emulator & tunnels
ALLOWED_HOSTS = [
    "api.afterclass.bond",
    "afterclass.bond",
    "*.afterclass.bond",
    "localhost",
    "127.0.0.1",
    "[::1]",
    "::1",
    "testserver",
    "10.0.2.2",
    "*.trycloudflare.com",
    "*.ngrok-free.app",
    "*.ngrok.io",
]

try:
    _lan = network_intel.get_lan_ip()
    if _lan and _lan not in ALLOWED_HOSTS:
        ALLOWED_HOSTS.append(_lan)
except Exception:
    pass

_env_allowed = os.getenv("ALLOWED_HOSTS")
if _env_allowed:
    for _h in _env_allowed.split(","):
        _h = _h.strip()
        if _h and _h not in ALLOWED_HOSTS:
            ALLOWED_HOSTS.append(_h)

app.add_middleware(StrictHostValidationMiddleware, allowed_hosts=ALLOWED_HOSTS)

# CORS Middleware (securely enables permanent domain https://afterclass.bond and local dev)
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "https://afterclass.bond",
        "http://localhost:8000",
        "http://127.0.0.1:8000",
    ],
    allow_origin_regex=r"^https://([a-zA-Z0-9-]+\.)?afterclass\.bond$",
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# HTTP Response Compression for JSON, HTML & SVG (> 500 bytes)
app.add_middleware(GZipMiddleware, minimum_size=500)

# Mount static, upload, and update directories
app.mount("/static", StaticFiles(directory=str(config.BASE_DIR / "static")), name="static")
app.mount("/uploads", StaticFiles(directory=str(config.UPLOAD_DIR)), name="uploads")
app.mount("/updates", StaticFiles(directory=str(config.UPDATES_DIR)), name="updates")

templates = Jinja2Templates(directory=str(config.BASE_DIR / "templates"))

# Rate limiting and brute force tracking in memory
failed_login_attempts: Dict[str, List[float]] = defaultdict(list)
upload_tracker: Dict[str, List[float]] = defaultdict(list)

# Security Headers & Anti-Fingerprinting Middleware
@app.middleware("http")
async def security_headers_middleware(request: Request, call_next):
    response = await call_next(request)
    # Strip server headers identifying local software / uvicorn
    response.headers["Server"] = "anonchat"
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["X-XSS-Protection"] = "1; mode=block"
    response.headers["Referrer-Policy"] = "no-referrer"
    response.headers["Permissions-Policy"] = "camera=(), microphone=(), geolocation=()"
    response.headers["X-Robots-Tag"] = "noindex, nofollow, noarchive"
    # Content Security Policy (allows app resources, prevents script injection and external exfiltration)
    csp = (
        "default-src 'self'; "
        "img-src 'self' data: blob: https:; "
        "style-src 'self' 'unsafe-inline'; "
        "script-src 'self' 'unsafe-inline'; "
        "connect-src 'self' ws: wss:; "
        "font-src 'self' data:; "
        "frame-ancestors 'none';"
    )
    response.headers["Content-Security-Policy"] = csp

    # Static and media asset caching
    if request.url.path.startswith("/static/"):
        response.headers.setdefault("Cache-Control", "public, max-age=86400")
    elif request.url.path.startswith("/uploads/"):
        response.headers.setdefault("Cache-Control", "public, max-age=604800, immutable")

    return response

@app.on_event("startup")
async def startup_event():
    database.init_db()

def get_client_ip(request: Request) -> str:
    """Safely extract client IP address, sanitizing against spoofing."""
    client_host = request.client.host if request.client else "127.0.0.1"
    # Only trust proxy headers if request arrived from local loopback (cloudflared tunnel)
    if client_host in ("127.0.0.1", "::1"):
        cf_ip = request.headers.get("cf-connecting-ip")
        if cf_ip and network_intel.is_valid_ip(cf_ip.strip()):
            return cf_ip.strip()
        forwarded = request.headers.get("x-forwarded-for")
        if forwarded:
            first_ip = forwarded.split(",")[0].strip()
            if network_intel.is_valid_ip(first_ip):
                return first_ip
    return client_host if network_intel.is_valid_ip(client_host) else "127.0.0.1"

def is_host_client(client_ip: str = None, cookies: dict = None, request: Request = None) -> bool:
    """
    Strict admin verification.
    Admin privileges require explicit authentication with ADMIN_TOKEN_SECRET.
    Loopback IP addresses (127.0.0.1, ::1) NEVER automatically grant admin privileges.
    """
    token = None

    # 1. Check cookies dict
    if cookies and isinstance(cookies, dict):
        if cookies.get("host_logged_out") == "1":
            return False
        token = cookies.get("host_verified")

    # 2. Check request cookies & headers
    if not token and request is not None:
        if request.cookies.get("host_logged_out") == "1":
            return False
        token = request.cookies.get("host_verified")

        if not token:
            auth_header = request.headers.get("authorization", "")
            if auth_header.startswith("Bearer "):
                token = auth_header[7:].strip()
            elif auth_header.startswith("Token "):
                token = auth_header[6:].strip()

        if not token:
            token = request.headers.get("x-admin-token")

    if not token:
        return False

    return hmac.compare_digest(str(token), str(config.ADMIN_TOKEN_SECRET))

def generate_public_id(identity_id: str) -> str:
    salt = config.ADMIN_TOKEN_SECRET[:16]
    raw = f"{identity_id}:{salt}".encode("utf-8")
    return hashlib.sha256(raw).hexdigest()[:8]

# Real-time WebSocket connection manager optimized for 1000+ concurrent users with bounded send queues
class ConnectionManager:
    def __init__(self):
        self.active_connections: Dict[WebSocket, dict] = {}
        # In-memory room membership index: room_id -> set of active WebSockets
        self.room_members: Dict[str, Set[WebSocket]] = defaultdict(set)
        # Reverse mapping: websocket -> set of room_ids
        self.socket_rooms: Dict[WebSocket, Set[str]] = defaultdict(set)
        # Fast lookup set of host sockets
        self.host_sockets: Set[WebSocket] = set()
        # Per-connection bounded queues and dedicated writer tasks
        self.queues: Dict[WebSocket, asyncio.Queue] = {}
        self.writer_tasks: Dict[WebSocket, asyncio.Task] = {}
        self.pending_invites: Dict[str, dict] = {}
        self._last_count_broadcast: float = 0.0
        self._count_task: Optional[asyncio.Task] = None
        self._reaper_task: Optional[asyncio.Task] = None

    def can_connect(self, client_ip: str) -> tuple[bool, str]:
        if len(self.active_connections) >= config.MAX_TOTAL_CONNECTIONS:
            return False, "Chat server is at full capacity (600+ users). Please wait a moment."
        ip_count = sum(1 for info in self.active_connections.values() if info.get("ip") == client_ip)
        if ip_count >= config.MAX_CONNECTIONS_PER_IP:
            return False, "Too many active connections from your IP."
        return True, ""

    async def connect(self, websocket: WebSocket, ip: str, session_id: str, public_id: str, anon_name: str, anon_avatar: str, sci_name: str, is_host: bool, user_agent: str):
        await websocket.accept()
        now = time.time()
        self.active_connections[websocket] = {
            "ip": ip,
            "session_id": session_id,
            "public_id": public_id,
            "anon_name": anon_name,
            "anon_avatar": anon_avatar,
            "sci_name": sci_name,
            "is_host": is_host,
            "user_agent": user_agent,
            "connected_at": now,
            "last_seen": now,
            "last_post_time": 0.0,
            "last_typing_time": 0.0,
            "recent_reactions": []
        }
        if is_host:
            self.host_sockets.add(websocket)

        if self._reaper_task is None or self._reaper_task.done():
            self._reaper_task = asyncio.create_task(self._heartbeat_reaper())

        # Start bounded write queue and dedicated non-blocking writer task
        q = asyncio.Queue(maxsize=100)
        self.queues[websocket] = q
        self.writer_tasks[websocket] = asyncio.create_task(self._writer_loop(websocket, q))

        # Index into all public streams
        for pub_room in database.PUBLIC_ROOMS:
            self.join_room(websocket, pub_room)

        # Index into user's existing private rooms
        private_room_ids = await asyncio.to_thread(database.get_user_room_ids, public_id)
        for r_id in private_room_ids:
            self.join_room(websocket, r_id)

        self.trigger_debounced_online_count()

    async def _writer_loop(self, ws: WebSocket, q: asyncio.Queue):
        """Dedicated writer task per WebSocket connection to eliminate broadcast blocking."""
        try:
            while True:
                msg = await q.get()
                if msg is None:
                    break
                try:
                    await asyncio.wait_for(ws.send_text(msg), timeout=5.0)
                except Exception:
                    # Client write timeout or broken pipe
                    break
                finally:
                    q.task_done()
        except asyncio.CancelledError:
            pass
        except Exception:
            pass
        finally:
            self.disconnect(ws)

    async def _heartbeat_reaper(self):
        """Periodically reap connections that have gone silent (no activity or ping for >45s)."""
        while True:
            try:
                await asyncio.sleep(10.0)
                now = time.time()
                dead = []
                for ws, info in list(self.active_connections.items()):
                    if now - info.get("last_seen", now) > 45.0:
                        dead.append(ws)
                for ws in dead:
                    self.disconnect(ws)
            except asyncio.CancelledError:
                break
            except Exception:
                pass

    def join_room(self, websocket: WebSocket, room_id: str):
        self.room_members[room_id].add(websocket)
        self.socket_rooms[websocket].add(room_id)

    def leave_room(self, websocket: WebSocket, room_id: str):
        if room_id in self.room_members:
            self.room_members[room_id].discard(websocket)
            if not self.room_members[room_id]:
                del self.room_members[room_id]
        if websocket in self.socket_rooms:
            self.socket_rooms[websocket].discard(room_id)

    def join_user_to_room(self, target_public_id: str, room_id: str):
        for ws, info in self.active_connections.items():
            if info.get("public_id") == target_public_id:
                self.join_room(ws, room_id)

    def leave_user_from_room(self, target_public_id: str, room_id: str):
        for ws, info in self.active_connections.items():
            if info.get("public_id") == target_public_id:
                self.leave_room(ws, room_id)

    def disconnect(self, websocket: WebSocket):
        if websocket not in self.active_connections and websocket not in self.queues:
            return
        info = self.active_connections.pop(websocket, None)
        self.host_sockets.discard(websocket)

        # Clean up all room memberships for this connection
        rooms = self.socket_rooms.pop(websocket, set())
        for r_id in rooms:
            if r_id in self.room_members:
                self.room_members[r_id].discard(websocket)
                if not self.room_members[r_id]:
                    del self.room_members[r_id]

        # Clean up queue and writer task
        self.queues.pop(websocket, None)
        task = self.writer_tasks.pop(websocket, None)
        if task and not task.done():
            task.cancel()

        try:
            asyncio.create_task(websocket.close())
        except Exception:
            pass

        # If user has no remaining connections, broadcast user_disconnected immediately
        public_id = info.get("public_id") if info else None
        if public_id:
            has_remaining = any(c.get("public_id") == public_id for c in self.active_connections.values())
            if not has_remaining:
                user_left_msg = json.dumps({"type": "user_disconnected", "public_id": public_id})
                asyncio.create_task(self._send_batch(list(self.active_connections.keys()), user_left_msg))

        self.trigger_debounced_online_count()

    def send_personal(self, websocket: WebSocket, data: Any) -> bool:
        """Enqueue a message to a specific connection safely and non-blockingly."""
        text = json.dumps(data) if isinstance(data, dict) else str(data)
        q = self.queues.get(websocket)
        if q is not None:
            try:
                q.put_nowait(text)
                return True
            except asyncio.QueueFull:
                self.disconnect(websocket)
                return False
        return False

    async def _send_batch(self, websockets: Iterable[WebSocket], text: str):
        """Instantly enqueue text to websockets without blocking the caller."""
        dead = []
        for ws in websockets:
            q = self.queues.get(ws)
            if q is not None:
                try:
                    q.put_nowait(text)
                except asyncio.QueueFull:
                    dead.append(ws)
        for ws in dead:
            self.disconnect(ws)

    def trigger_debounced_online_count(self):
        """Debounce online count broadcasts so 500 users connecting doesn't flood CPU."""
        now = time.time()
        if now - self._last_count_broadcast >= 2.0:
            self._last_count_broadcast = now
            asyncio.create_task(self.broadcast_online_count())
        elif self._count_task is None or self._count_task.done():
            async def _delayed():
                await asyncio.sleep(2.0)
                self._last_count_broadcast = time.time()
                await self.broadcast_online_count()
            self._count_task = asyncio.create_task(_delayed())

    def get_online_users(self, is_host: bool = False) -> List[dict]:
        seen = {}
        for ws, info in list(self.active_connections.items()):
            pid = info.get("public_id")
            if pid and pid not in seen:
                u = {
                    "public_id": pid,
                    "anon_name": info.get("anon_name"),
                    "anon_avatar": info.get("anon_avatar"),
                    "sci_name": info.get("sci_name", "")
                }
                if is_host:
                    u["ip"] = info.get("ip")
                    ident = database.get_identity(info.get("ip"))
                    u["real_name"] = ident.get("real_name") if ident else ""
                seen[pid] = u
        return list(seen.values())

    async def send_to_user(self, target_public_id: str, data: dict) -> bool:
        msg = json.dumps(data)
        targets = [ws for ws, info in list(self.active_connections.items()) if info.get("public_id") == target_public_id]
        if targets:
            await self._send_batch(targets, msg)
            return True
        return False

    async def broadcast_to_room(self, room_id: str, data: dict):
        msg = json.dumps(data)
        targets = list(self.room_members.get(room_id, set()) | self.host_sockets)
        await self._send_batch(targets, msg)

    async def broadcast_online_count(self):
        count = len(self.active_connections)
        users = self.get_online_users(is_host=False)
        msg = json.dumps({"type": "online_count", "count": count, "users": users})
        await self._send_batch(list(self.active_connections.keys()), msg)

    async def broadcast_message(self, full_msg: dict):
        room_id = full_msg.get("room_id") or "main"
        public_msg = dict(full_msg)
        # CRITICAL PRIVACY FIX: Strip session_id, IP, MAC, hostname, real name
        public_msg.pop("session_id", None)
        public_msg.pop("ip", None)
        public_msg.pop("hostname", None)
        public_msg.pop("mac", None)
        public_msg.pop("user_agent", None)
        public_msg.pop("device_summary", None)
        public_msg.pop("real_name", None)
        public_msg.pop("notes", None)
        public_msg.pop("is_banned", None)
        public_msg.pop("_is_duplicate", None)

        # Host payload also strips internal deduplication flag
        host_msg = dict(full_msg)
        host_msg.pop("_is_duplicate", None)

        public_json = json.dumps({"type": "new_message", "message": public_msg})
        host_json = json.dumps({"type": "new_message", "message": host_msg})

        # Separate room recipients and host recipients to avoid duplicate transmissions
        room_targets = [ws for ws in self.room_members.get(room_id, set()) if ws not in self.host_sockets]
        host_targets = list(self.host_sockets)

        await asyncio.gather(
            self._send_batch(host_targets, host_json),
            self._send_batch(room_targets, public_json),
            return_exceptions=True
        )

    async def broadcast_delete(self, post_num: int):
        msg = json.dumps({"type": "delete_message", "post_num": post_num})
        await self._send_batch(list(self.active_connections.keys()), msg)

    async def broadcast_clear(self):
        msg = json.dumps({"type": "clear_chat"})
        await self._send_batch(list(self.active_connections.keys()), msg)

    async def broadcast_reaction(self, post_num: int, reactions: dict):
        msg = json.dumps({"type": "reaction_update", "post_num": post_num, "reactions": reactions})
        await self._send_batch(list(self.active_connections.keys()), msg)

    async def broadcast_typing(self, animal_name: str, sender_ws: WebSocket, public_id: str = "", session_id: str = "", room_id: Optional[str] = None):
        payload = {"type": "typing", "animal_name": animal_name, "public_id": public_id, "session_id": session_id}
        if room_id:
            payload["room_id"] = room_id
        msg = json.dumps(payload)
        recipients = [ws for ws in list(self.active_connections.keys()) if ws != sender_ws]
        await self._send_batch(recipients, msg)

    async def broadcast_identity_update(self, ip: str, real_name: str, notes: str):
        msg = json.dumps({
            "type": "update_identity",
            "ip": ip,
            "real_name": real_name,
            "notes": notes
        })
        targets = list(self.host_sockets)
        await self._send_batch(targets, msg)

    async def broadcast_purge(self, ip: str, post_nums: Optional[List[int]] = None):
        # Notify host with full IP
        host_msg = json.dumps({"type": "purge_ip", "ip": ip})
        host_targets = list(self.host_sockets)
        await self._send_batch(host_targets, host_msg)

        # Notify non-host users only with deleted post numbers (ZERO visitor IP leak!)
        if post_nums:
            pub_msg = json.dumps({"type": "posts_deleted", "post_nums": post_nums})
            pub_targets = [ws for ws in list(self.active_connections.keys()) if ws not in self.host_sockets]
            await self._send_batch(pub_targets, pub_msg)

    async def broadcast_announcement(self, text: str):
        msg = json.dumps({"type": "announcement", "text": text})
        await self._send_batch(list(self.active_connections.keys()), msg)

    async def broadcast_system_announcement(self, bc_data: dict):
        room_id = bc_data.get("room_id")
        if room_id and str(room_id).strip() not in ("", "main", "global", "all"):
            await self.broadcast_to_room(str(room_id).strip(), bc_data)
        else:
            await self.broadcast(bc_data)

    async def broadcast(self, data: dict):
        msg = json.dumps(data)
        await self._send_batch(list(self.active_connections.keys()), msg)

    async def kick_ip(self, ip: str):
        for ws, info in list(self.active_connections.items()):
            if info.get("ip") == ip:
                self.send_personal(ws, {"type": "error", "message": "You have been disconnected by the host."})
                self.disconnect(ws)

    async def broadcast_ai_event(self, event_data: dict):
        msg = json.dumps({"type": "ai_moderation_event", "event": event_data})
        host_targets = list(self.host_sockets)
        await self._send_batch(host_targets, msg)

manager = ConnectionManager()



# Routes
# Routes
@app.get("/", response_class=HTMLResponse)
async def index(request: Request, response: Response):
    client_ip = get_client_ip(request)
    is_host = is_host_client(client_ip, request.cookies, request=request)

    # CRITICAL PRIVACY: Never expose host laptop LAN IP to non-host visitors!
    lan_ip = network_intel.get_lan_ip() if is_host else ""

    # Generate fresh session_id on each reload/rejoin (giving user a new animal and ID)
    session_id = "sess_" + uuid.uuid4().hex[:16]

    public_url = tunnel.get_public_url()
    cache_v = int(time.time())

    res = templates.TemplateResponse("index.html", {
        "request": request,
        "lan_ip": lan_ip,
        "port": config.PORT,
        "is_host": is_host,
        "client_ip": client_ip,
        "session_id": session_id,
        "public_url": public_url,
        "cache_v": cache_v
    })
    res.headers["Cache-Control"] = "no-cache, no-store, must-revalidate"
    res.headers["Pragma"] = "no-cache"
    res.headers["Expires"] = "0"
    res.set_cookie("session_id", session_id, max_age=365*86400, httponly=False, samesite="lax")
    return res

@app.get("/admin", response_class=HTMLResponse)
@app.get("/host", response_class=HTMLResponse)
async def admin_page(request: Request, response: Response):
    return await index(request, response)

@app.get("/api/animal/{slug}")
async def get_animal_image(slug: str):
    clean_slug = re.sub(r'[^a-zA-Z0-9_-]', '', slug)[:40]
    data = await asyncio.to_thread(animal_avatars.get_cached_or_fetch_avatar, clean_slug)
    media_type = "image/svg+xml" if data.startswith(b"<svg") else "image/jpeg"
    return RawResponse(content=data, media_type=media_type, headers={"Cache-Control": "public, max-age=31536000, immutable"})

@app.get("/api/messages")
async def api_messages(request: Request, room_id: str = "main", limit: int = 50, before_post_num: Optional[int] = None):
    client_ip = get_client_ip(request)
    is_host = is_host_client(client_ip, request.cookies, request=request)
    session_id = request.query_params.get("session_id") or request.cookies.get("session_id") or ""
    identity_id = database.get_or_create_identity_id(session_id)
    public_id = generate_public_id(identity_id)
    clean_room = re.sub(r'[^a-zA-Z0-9_-]', '', room_id)[:50] or "main"
    safe_limit = min(max(int(limit), 1), 200)

    if clean_room not in database.PUBLIC_ROOMS and not is_host:
        # Check if this is a user-created public group (is_public=1 in rooms table)
        room_info = await asyncio.to_thread(database.get_room_info, clean_room)
        is_public_group = room_info and bool(room_info.get("is_public"))

        if not database.is_user_in_room(clean_room, public_id, is_host):
            if is_public_group:
                # Public group: auto-join the user so they can read messages
                animal = animal_avatars.get_animal_for_public_id(public_id)
                await asyncio.to_thread(database.add_user_to_room, clean_room, public_id, animal["handle"], animal["avatar_url"])
            else:
                # Private group: block access if not a member
                raise HTTPException(status_code=403, detail="Not a participant in this room")

    manager.join_user_to_room(public_id, clean_room)
    messages = database.get_messages(room_id=clean_room, limit=safe_limit, is_host=is_host, before_post_num=before_post_num)
    return {"status": "ok", "room_id": clean_room, "messages": messages, "is_host": is_host}

@app.get("/api/rooms")
async def api_rooms(request: Request):
    client_ip = get_client_ip(request)
    is_host = is_host_client(client_ip, request.cookies, request=request)
    session_id = request.query_params.get("session_id") or request.cookies.get("session_id") or ""
    identity_id = database.get_or_create_identity_id(session_id)
    public_id = generate_public_id(identity_id)
    rooms = database.get_user_rooms(public_id, is_host=is_host)
    return {"status": "ok", "rooms": rooms}

@app.post("/api/rooms/create")
async def api_create_room(request: Request):
    client_ip = get_client_ip(request)
    if database.is_ip_banned(client_ip):
        raise HTTPException(status_code=403, detail="Banned from creating rooms")
    if database.is_ip_muted(client_ip):
        raise HTTPException(status_code=403, detail="Muted from creating rooms")

    try:
        data = await request.json()
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid JSON payload")

    session_id = str(data.get("session_id") or request.query_params.get("session_id") or request.cookies.get("session_id") or "").strip()
    if not session_id:
        raise HTTPException(status_code=400, detail="Session ID is required")

    name = str(data.get("name") or "").strip()
    if len(name) < 2 or len(name) > 32:
        raise HTTPException(status_code=400, detail="Group name must be between 2 and 32 characters")

    desc = str(data.get("desc") or data.get("description") or "").strip()[:140]
    is_public = bool(data.get("is_public", True))

    identity_id = database.get_or_create_identity_id(session_id)
    public_id = generate_public_id(identity_id)
    animal = animal_avatars.get_animal_for_public_id(public_id)

    # Check for duplicate active room with exact same name
    conn = database.get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT room_id FROM rooms WHERE LOWER(name) = LOWER(?) AND is_active = 1", (name,))
    existing = cur.fetchone()
    conn.close()
    if existing:
        raise HTTPException(status_code=409, detail=f"A group named '{name}' already exists. Please choose a different name.")

    clean_slug = re.sub(r'[^a-zA-Z0-9_-]', '', name.lower().replace(' ', '_'))[:16]
    if not clean_slug:
        clean_slug = "group"
    room_id = f"grp_{clean_slug}_{uuid.uuid4().hex[:6]}"

    created_room = database.create_group_room(
        room_id=room_id,
        name=name,
        desc=desc,
        owner_public_id=public_id,
        creator_anon_name=animal["handle"],
        creator_anon_avatar=animal["avatar_url"],
        is_public=is_public
    )

    manager.join_user_to_room(public_id, room_id)

    return {
        "status": "ok",
        "room": created_room
    }

@app.post("/api/rooms/update_name")
async def api_update_room_name(request: Request):
    try:
        data = await request.json()
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid JSON body")

    session_id = str(data.get("session_id") or "").strip()
    room_id = str(data.get("room_id") or "").strip()
    new_name = str(data.get("name") or "").strip()

    if not session_id or not room_id or not new_name:
        raise HTTPException(status_code=400, detail="session_id, room_id, and name are required")

    if len(new_name) < 2 or len(new_name) > 32:
        raise HTTPException(status_code=400, detail="Group name must be between 2 and 32 characters")

    identity_id = database.get_or_create_identity_id(session_id)
    public_id = generate_public_id(identity_id)
    animal = animal_avatars.get_animal_for_public_id(public_id)

    success = database.update_room_name(room_id, new_name, public_id)
    if not success:
        raise HTTPException(status_code=403, detail="Room not found or not authorized to rename")

    # Insert a system message informing the group
    sys_msg = await asyncio.to_thread(
        database.insert_message,
        session_id="system",
        public_id="system",
        anon_name="SYSTEM",
        anon_avatar="/api/animal/snow_leopard",
        ip="127.0.0.1",
        hostname="localhost",
        mac="",
        user_agent="",
        device_summary="",
        content=f"{animal['handle']} changed the group name to '{new_name}'.",
        room_id=room_id
    )

    members = await asyncio.to_thread(database.get_room_members, room_id)
    room_info = {
        "room_id": room_id,
        "name": new_name,
        "members": members
    }

    # Broadcast room_updated event to all members and globally so all devices update instantly
    await manager.broadcast({
        "type": "room_updated",
        "room": room_info,
        "system_message": sys_msg
    })

    return {"status": "ok", "name": new_name, "room": room_info}

@app.post("/api/rooms/update_desc")
async def api_update_room_desc(request: Request):
    try:
        data = await request.json()
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid JSON body")

    session_id = str(data.get("session_id") or "").strip()
    room_id = str(data.get("room_id") or "").strip()
    new_desc = str(data.get("desc") or data.get("description") or "").strip()[:140]

    if not session_id or not room_id:
        raise HTTPException(status_code=400, detail="session_id and room_id are required")

    identity_id = database.get_or_create_identity_id(session_id)
    public_id = generate_public_id(identity_id)

    success = database.update_room_desc(room_id, new_desc, public_id)
    if not success:
        raise HTTPException(status_code=403, detail="Room not found or not authorized to update description")

    members = await asyncio.to_thread(database.get_room_members, room_id)
    room_info = {
        "room_id": room_id,
        "desc": new_desc,
        "members": members
    }

    await manager.broadcast({
        "type": "room_updated",
        "room": room_info
    })

    return {"status": "ok", "desc": new_desc, "room": room_info}

@app.post("/api/rooms/update_visibility")
async def api_update_room_visibility(request: Request):
    try:
        data = await request.json()
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid JSON body")

    session_id = str(data.get("session_id") or "").strip()
    room_id = str(data.get("room_id") or "").strip()
    is_public = bool(data.get("is_public", True))

    if not session_id or not room_id:
        raise HTTPException(status_code=400, detail="session_id and room_id are required")

    identity_id = database.get_or_create_identity_id(session_id)
    public_id = generate_public_id(identity_id)

    success = database.update_room_visibility(room_id, is_public, public_id)
    if not success:
        raise HTTPException(status_code=403, detail="Room not found or not authorized to change visibility")

    members = await asyncio.to_thread(database.get_room_members, room_id)
    room_info = {
        "room_id": room_id,
        "is_public": is_public,
        "members": members
    }

    await manager.broadcast({
        "type": "room_updated",
        "room": room_info
    })

    return {"status": "ok", "is_public": is_public, "room": room_info}

@app.get("/api/room_members")
async def api_room_members(request: Request):
    room_id = request.query_params.get("room_id") or ""
    if not room_id:
        raise HTTPException(status_code=400, detail="room_id is required")
    members = await asyncio.to_thread(database.get_room_members, room_id)
    return {"status": "ok", "room_id": room_id, "members": members}

@app.post("/api/rooms/delete")
async def api_delete_group_room(request: Request):
    try:
        data = await request.json()
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid JSON body")

    session_id = str(data.get("session_id") or "").strip() or get_session_id(request)
    room_id = str(data.get("room_id") or "").strip()

    if not session_id or not room_id:
        raise HTTPException(status_code=400, detail="session_id and room_id are required")

    identity_id = database.get_or_create_identity_id(session_id)
    public_id = generate_public_id(identity_id)

    success, result_or_err = await asyncio.to_thread(database.delete_group_room, room_id, public_id)
    if not success:
        raise HTTPException(status_code=403, detail=result_or_err)

    room_name = result_or_err
    await manager.broadcast({
        "type": "room_deleted",
        "room_id": room_id,
        "room_name": room_name,
        "reason": f"Group '{room_name}' was deleted by group admin."
    })
    return {"status": "ok", "room_id": room_id, "room_name": room_name}

@app.get("/api/online_users")
async def api_online_users(request: Request):
    client_ip = get_client_ip(request)
    is_host = is_host_client(client_ip, request.cookies, request=request)
    users = manager.get_online_users(is_host=is_host)
    return {"status": "ok", "users": users}

def _process_and_save_upload(contents: bytes, ext: str) -> tuple[str, str, str]:
    raw_img = Image.open(io.BytesIO(contents))
    raw_img.verify()

    file_id = f"{int(time.time())}_{uuid.uuid4().hex[:8]}"

    if ext == ".gif":
        # For animated GIFs, write raw validated bytes directly to preserve 100% of frames, duration, and looping
        saved_filename = f"{file_id}.gif"
        orig_path = config.UPLOAD_DIR / saved_filename
        with open(orig_path, "wb") as f:
            f.write(contents)
        thumb_filename = saved_filename
        img = Image.open(io.BytesIO(contents))
        dims = f"{img.width}x{img.height}"
    else:
        # Re-open for clean processing
        img = Image.open(io.BytesIO(contents))
        w, h = img.size
        dims = f"{w}x{h}"

        # Transpose according to EXIF orientation then strip EXIF completely
        try:
            img = ImageOps.exif_transpose(img)
        except Exception:
            pass

        # High-performance WebP compression (saves 60-80% disk & mobile bandwidth)
        saved_filename = f"{file_id}.webp"
        orig_path = config.UPLOAD_DIR / saved_filename
        thumb_filename = f"{file_id}_thumb.webp"
        thumb_path = config.THUMBNAIL_DIR / thumb_filename

        # Convert to RGB or RGBA for clean WebP encoding
        if img.mode not in ("RGB", "RGBA"):
            img = img.convert("RGBA" if "transparency" in img.info or img.mode == "P" else "RGB")

        # Save main WebP image with quality=80
        img.save(orig_path, format="WEBP", quality=80, method=4)

        # Generate WebP thumbnail (max 450x450, quality=75)
        img_thumb = img.copy()
        img_thumb.thumbnail((450, 450))
        img_thumb.save(thumb_path, format="WEBP", quality=75, method=4)

    return saved_filename, thumb_filename, dims

@app.post("/api/upload")
async def upload_file(request: Request, file: UploadFile = File(...)):
    client_ip = get_client_ip(request)
    if database.is_ip_banned(client_ip):
        raise HTTPException(status_code=403, detail="Banned from uploading")

    # Rate limiting on file uploads (prevents storage & bandwidth exhaustion)
    now = time.time()
    upload_tracker[client_ip] = [t for t in upload_tracker[client_ip] if now - t < 60.0]
    if len(upload_tracker[client_ip]) >= config.RATE_LIMIT_UPLOAD_PER_MIN:
        raise HTTPException(status_code=429, detail="Upload limit reached (max 5 images/minute). Please slow down.")
    upload_tracker[client_ip].append(now)

    ext = os.path.splitext(file.filename or "")[1].lower()
    if ext not in config.ALLOWED_EXTENSIONS:
        raise HTTPException(status_code=400, detail="Only JPG, PNG, GIF, and WEBP supported")

    contents = await file.read()
    if len(contents) > config.MAX_UPLOAD_SIZE:
        raise HTTPException(status_code=400, detail="File too large (Max 8MB)")

    # Sanitize and strip EXIF (removes GPS coordinates, device serial numbers, camera model)
    try:
        saved_filename, thumb_filename, dims = await asyncio.to_thread(_process_and_save_upload, contents, ext)
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid or corrupted image format")

    safe_name = re.sub(r'[^a-zA-Z0-9._ -]', '', os.path.basename(file.filename or "image.jpg"))[:60]
    thumb_url = f"/uploads/{saved_filename}" if ext == ".gif" else f"/uploads/thumbs/{thumb_filename}"
    return {
        "status": "ok",
        "image_path": f"/uploads/{saved_filename}",
        "image_thumb": thumb_url,
        "image_name": safe_name,
        "image_size": len(contents),
        "image_dims": dims
    }

@app.get("/api/stickers_and_gifs")
async def get_stickers_and_gifs():
    stickers_dir = config.BASE_DIR / "static" / "stickers"
    gifs_dir = config.BASE_DIR / "static" / "gifs"

    stickers = []
    if stickers_dir.exists():
        for f in sorted(stickers_dir.glob("*.svg")):
            category = "Reactions"
            if f.name in ("attendance_warning.svg", "bunk_class.svg", "degree_loading.svg", "coffee_fuel.svg", "canteen_chai.svg", "code_bug_404.svg"):
                category = "Campus"
            elif f.name in ("capybara_chill.svg", "cat_vibing.svg", "doge_secret.svg"):
                category = "Mascots"

            clean_name = f.stem.replace("_", " ").title()
            stickers.append({
                "name": clean_name,
                "path": f"/static/stickers/{f.name}",
                "category": category
            })

    gifs = []
    if gifs_dir.exists():
        for f in sorted(gifs_dir.glob("*.gif")):
            clean_name = f.stem.replace("_", " ").title()
            gifs.append({
                "name": clean_name,
                "path": f"/static/gifs/{f.name}"
            })

    return {
        "status": "ok",
        "stickers": stickers,
        "gifs": gifs
    }

def resolve_media_to_direct_url(url: str) -> str:
    if not url or not isinstance(url, str):
        return url
    url_clean = url.strip()
    try:
        parsed = urllib.parse.urlparse(url_clean)
        scheme = parsed.scheme.lower()
        if scheme not in ("http", "https"):
            return url_clean
        hostname = (parsed.hostname or "").lower()
    except Exception:
        return url_clean

    is_tenor = hostname == "tenor.com" or hostname.endswith(".tenor.com")
    is_giphy = hostname == "giphy.com" or hostname.endswith(".giphy.com")
    if not (is_tenor or is_giphy):
        return url_clean

    # Already a direct file or direct cdn media
    if "media.tenor.com" in hostname or "c.tenor.com" in hostname or url_clean.endswith(".gif"):
        return url_clean

    if is_tenor and "/view/" in parsed.path:
        try:
            req = urllib.request.Request(
                url_clean,
                headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"}
            )
            with urllib.request.urlopen(req, timeout=3) as resp:
                html = resp.read().decode("utf-8", errors="ignore")
                media = re.findall(r'https://media\.tenor\.com/[^"\' ]+\.gif', html)
                if media:
                    return media[0]
                og = re.findall(r'<meta[^>]+property=["\']og:image["\'][^>]+content=["\']([^"\']+)["\']', html)
                if og:
                    return og[0]
        except Exception:
            pass
    elif is_giphy and "/gifs/" in parsed.path:
        try:
            req = urllib.request.Request(
                url_clean,
                headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"}
            )
            with urllib.request.urlopen(req, timeout=3) as resp:
                html = resp.read().decode("utf-8", errors="ignore")
                media = re.findall(r'https://media[0-9]*\.giphy\.com/media/[^"\' ]+/giphy\.gif', html)
                if media:
                    return media[0]
                og = re.findall(r'<meta[^>]+property=["\']og:image["\'][^>]+content=["\']([^"\']+)["\']', html)
                if og:
                    return og[0]
        except Exception:
            pass
    return url_clean

def extract_media_url_from_text(text: str) -> Optional[str]:
    if not text or not isinstance(text, str):
        return None
    pattern = r'(https?://[^\s<>]+?\.(?:gif|webp|png|jpe?g|svg)(?:\?[^\s<>]*)?|https?://(?:[a-zA-Z0-9.-]+\.)?(?:tenor\.com|giphy\.com|klipy\.com)/[^\s<>]+)'
    m = re.search(pattern, text, re.IGNORECASE)
    if m:
        return m.group(1)
    return None

@app.post("/api/resolve_media")
async def resolve_media_endpoint(request: Request):
    try:
        data = await request.json()
    except Exception:
        data = {}
    raw_url = (data.get("url") or "").strip()
    if not raw_url:
        return {"status": "error", "message": "No URL provided"}
    resolved = resolve_media_to_direct_url(raw_url)
    return {"status": "ok", "original": raw_url, "resolved": resolved}

@app.get("/api/qrcode")
async def generate_qr(request: Request, custom_url: Optional[str] = None):
    client_ip = get_client_ip(request)
    is_host = is_host_client(client_ip, request.cookies, request=request)
    url = custom_url
    if not url:
        public_url = tunnel.get_public_url()
        if public_url:
            url = public_url.rstrip("/") + "/"
        elif is_host:
            lan_ip = network_intel.get_lan_ip()
            port = config.PORT
            url = f"http://{lan_ip}:{port}/"
        else:
            url = str(request.base_url)

    if qrcode:
        qr = qrcode.QRCode(version=1, box_size=8, border=2)
        qr.add_data(url)
        qr.make(fit=True)
        img = qr.make_image(fill_color="#22272e", back_color="#ffffff")
        buf = io.BytesIO()
        img.save(buf, format="PNG")
        buf.seek(0)
        return StreamingResponse(buf, media_type="image/png")
    else:
        # Fallback to system qrencode binary
        try:
            res = subprocess.run(
                ["qrencode", "-o", "-", "-s", "8", "-m", "2", url],
                capture_output=True,
                check=True
            )
            return StreamingResponse(io.BytesIO(res.stdout), media_type="image/png")
        except Exception:
            img = Image.new("RGB", (200, 200), color="#ffffff")
            buf = io.BytesIO()
            img.save(buf, format="PNG")
            buf.seek(0)
            return StreamingResponse(buf, media_type="image/png")

@app.post("/api/leave_all_rooms")
async def leave_all_rooms(request: Request):
    client_ip = get_client_ip(request)
    session_id = request.query_params.get("session_id") or request.cookies.get("session_id") or ""
    identity_id = database.get_or_create_identity_id(session_id)
    public_id = generate_public_id(identity_id)
    deleted_rooms = database.remove_user_from_all_rooms(public_id)
    for r_id in deleted_rooms:
        await manager.broadcast_to_room(r_id, {
            "type": "room_deleted",
            "room_id": r_id,
            "message": "This group has been deleted because all members left."
        })
    return {"status": "ok", "deleted_rooms": deleted_rooms}

@app.get("/api/share_info")
async def get_share_info(request: Request):
    client_ip = get_client_ip(request)
    is_host = is_host_client(client_ip, request.cookies, request=request)
    public_url = tunnel.get_public_url()
    lan_ip = network_intel.get_lan_ip() if is_host else ""
    lan_url = f"http://{lan_ip}:{config.PORT}/" if (is_host and lan_ip) else None
    return {
        "status": "ok",
        "public_url": public_url,
        "lan_url": lan_url,
        "active_url": public_url if public_url else (lan_url or "")
    }

@app.post("/api/admin/login")
@app.post("/api/host/login")
async def admin_login(request: Request):
    client_ip = get_client_ip(request)

    # 1. Rate limiting & lockout check
    now = time.time()
    failed_login_attempts[client_ip] = [
        t for t in failed_login_attempts[client_ip]
        if now - t < config.RATE_LIMIT_LOGIN_LOCKOUT
    ]
    if len(failed_login_attempts[client_ip]) >= config.RATE_LIMIT_LOGIN_ATTEMPTS:
        raise HTTPException(
            status_code=429,
            detail=f"Too many failed login attempts. Locked out for {config.RATE_LIMIT_LOGIN_LOCKOUT // 60} minutes."
        )

    # 2. Extract password from JSON or form data
    password = ""
    try:
        content_type = request.headers.get("content-type", "")
        if "application/json" in content_type:
            body = await request.json()
            password = str(body.get("password", ""))
        else:
            form = await request.form()
            password = str(form.get("password", ""))
    except Exception:
        password = ""

    # 3. Constant-time password comparison
    if not password or not hmac.compare_digest(password.strip(), config.ADMIN_PASSWORD.strip()):
        failed_login_attempts[client_ip].append(now)
        raise HTTPException(status_code=401, detail="Invalid admin password")

    # 4. On success: clear failed attempts
    failed_login_attempts.pop(client_ip, None)

    # 5. Issue admin cookie and token response
    response = JSONResponse({
        "status": "ok",
        "message": "Admin authenticated successfully",
        "token": config.ADMIN_TOKEN_SECRET
    })
    response.set_cookie(
        "host_verified",
        config.ADMIN_TOKEN_SECRET,
        httponly=True,
        samesite="lax",
        max_age=86400 * 7
    )
    response.delete_cookie("host_logged_out")
    return response

@app.post("/api/admin/logout")
@app.post("/api/host/logout")
async def admin_logout():
    response = JSONResponse({"status": "ok", "message": "Admin logged out"})
    response.delete_cookie("host_verified")
    response.set_cookie("host_logged_out", "1", httponly=True, samesite="lax", max_age=86400 * 7)
    return response

# Host-Only API Endpoints (Guarded strictly by is_host_client with request inspection)
@app.get("/api/host/identities")
async def host_identities(request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    identities = database.get_all_identities()
    return {"status": "ok", "identities": identities}

@app.post("/api/host/assign_name")
async def host_assign_name(request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    data = await request.json()
    ip = data.get("ip")
    real_name = str(data.get("real_name", "")).strip()[:60]
    notes = data.get("notes")

    if not ip or not network_intel.is_valid_ip(ip):
        raise HTTPException(status_code=400, detail="Valid IP is required")

    database.assign_real_name(ip, real_name, notes)
    await manager.broadcast_identity_update(ip, real_name, notes or "")
    return {"status": "ok", "ip": ip, "real_name": real_name}

@app.post("/api/host/ban")
async def host_ban(request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    data = await request.json()
    ip = data.get("ip")
    banned = bool(data.get("banned", True))

    if not ip or not network_intel.is_valid_ip(ip):
        raise HTTPException(status_code=400, detail="Valid IP is required")

    database.set_ban_status(ip, banned)
    if banned:
        await manager.kick_ip(ip)
    return {"status": "ok", "ip": ip, "banned": banned}

@app.post("/api/host/ban_by_user")
async def host_ban_by_user(request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    data = await request.json()
    user_id = data.get("user_id")
    post_num = data.get("post_num")
    banned = bool(data.get("banned", True))
    
    target_ip = None
    if post_num:
        pinfo = database.get_post_info(int(post_num))
        if pinfo and pinfo.get("ip"):
            target_ip = pinfo["ip"]
    elif user_id:
        clean_id = str(user_id).strip().lstrip('#')
        target_ip = database.get_ip_by_public_id(clean_id)
        
    if not target_ip:
        raise HTTPException(status_code=404, detail="Could not resolve IP for specified user/post")
        
    database.set_ban_status(target_ip, banned)
    if banned:
        await manager.kick_ip(target_ip)
    return {"status": "ok", "ip": target_ip, "banned": banned}

@app.post("/api/host/mute")
async def host_mute(request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    data = await request.json()
    ip = data.get("ip")
    muted = bool(data.get("muted", True))
    minutes = int(data.get("minutes", 15))

    if not ip or not network_intel.is_valid_ip(ip):
        raise HTTPException(status_code=400, detail="Valid IP is required")

    database.set_mute_status(ip, muted, minutes)
    return {"status": "ok", "ip": ip, "muted": muted}

@app.post("/api/host/purge_ip")
async def host_purge_ip(request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    data = await request.json()
    ip = data.get("ip")

    if not ip or not network_intel.is_valid_ip(ip):
        raise HTTPException(status_code=400, detail="Valid IP is required")

    count, post_nums = database.delete_all_from_ip(ip)
    await manager.broadcast_purge(ip, post_nums)
    return {"status": "ok", "ip": ip, "deleted_count": count}

@app.post("/api/host/pin")
async def host_pin(request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    data = await request.json()
    try:
        post_num = int(data.get("post_num"))
    except (TypeError, ValueError):
        raise HTTPException(status_code=400, detail="Valid post_num is required")
    pinned = bool(data.get("pinned", True))

    database.set_pinned(post_num, pinned)
    return {"status": "ok", "post_num": post_num, "pinned": pinned}

@app.post("/api/host/announce")
async def host_announce(request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    data = await request.json()
    message = str(data.get("message", "")).strip()[:500]

    if message:
        await manager.broadcast_announcement(message)
    return {"status": "ok", "message": message}

_current_active_broadcast: Optional[dict] = None
_broadcast_expiry_time: float = 0.0

@app.get("/api/broadcast/active")
async def get_active_broadcast():
    global _current_active_broadcast, _broadcast_expiry_time
    if _current_active_broadcast:
        dur = _current_active_broadcast.get("duration", 0)
        if dur > 0 and time.time() > _broadcast_expiry_time:
            _current_active_broadcast = None
            return {"status": "ok", "broadcast": None}
        return {"status": "ok", "broadcast": _current_active_broadcast}
    return {"status": "ok", "broadcast": None}

@app.post("/api/host/broadcast")
@app.post("/api/admin/broadcast")
async def host_broadcast(request: Request):
    global _current_active_broadcast, _broadcast_expiry_time
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    data = await request.json()
    message = str(data.get("message", "")).strip()[:500]
    if not message:
        raise HTTPException(status_code=400, detail="Broadcast message cannot be empty")

    severity = str(data.get("severity", "INFO")).strip().upper()
    if severity not in ("INFO", "WARNING", "EMERGENCY"):
        severity = "INFO"

    room_id = str(data.get("room_id", "")).strip()
    target_audience = str(data.get("target_audience", "all_new")).strip()
    try:
        duration = int(data.get("duration", 0))
    except (ValueError, TypeError):
        duration = 0
    duration = max(0, min(duration, 86400))

    bc_id = f"bc_{int(time.time() * 1000)}_{uuid.uuid4().hex[:6]}"
    now_iso = datetime.now(timezone.utc).isoformat()

    bc_payload = {
        "type": "system_broadcast",
        "action": "system_broadcast",
        "broadcast_id": bc_id,
        "id": bc_id,
        "message": message,
        "severity": severity,
        "room_id": room_id,
        "target_audience": target_audience,
        "duration": duration,
        "created_at": now_iso
    }

    _current_active_broadcast = {
        "broadcast_id": bc_id,
        "message": message,
        "severity": severity,
        "room_id": room_id,
        "target_audience": target_audience,
        "duration": duration,
        "created_at": now_iso
    }
    _broadcast_expiry_time = (time.time() + duration) if duration > 0 else 0.0

    await manager.broadcast_system_announcement(bc_payload)
    return {"status": "ok", "broadcast": _current_active_broadcast}

@app.post("/api/host/delete/{post_num}")
async def host_delete(post_num: int, request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    database.delete_message(post_num)
    await manager.broadcast_delete(post_num)
    return {"status": "ok", "post_num": post_num}

@app.post("/api/host/clear")
async def host_clear(request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    database.clear_messages()
    await manager.broadcast_clear()
    return {"status": "ok", "message": "All messages cleared"}

@app.get("/api/host/rooms")
async def host_get_rooms(request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    rooms = database.get_all_rooms_admin()
    return {"status": "ok", "rooms": rooms}

@app.post("/api/host/delete_room/{room_id}")
async def host_delete_room(room_id: str, request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    if not room_id or room_id == "main":
        raise HTTPException(status_code=400, detail="Cannot delete main public room")
    
    conn = database.get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT name FROM rooms WHERE room_id = ?", (room_id,))
    row = cur.fetchone()
    conn.close()
    room_name = row["name"] if row else room_id

    database.delete_room(room_id)

    # Broadcast room deletion to all users
    await manager.broadcast({
        "type": "room_deleted",
        "room_id": room_id,
        "room_name": room_name,
        "reason": f"Group '{room_name}' was deleted by the host admin."
    })
    return {"status": "ok", "room_id": room_id, "room_name": room_name}
    

# ==========================================
# Host AI Assistant Endpoints (Qwen / Ollama)
# ==========================================
@app.get("/api/host/ai/status")
async def host_ai_status(request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    is_alive = await asyncio.to_thread(ai_admin.check_ollama_alive, 1.2)
    models = await asyncio.to_thread(ai_admin.get_available_models, 1.5) if is_alive else []
    return {
        "status": "ok",
        "ollama_online": is_alive,
        "models": models,
        "default_model": ai_admin.DEFAULT_MODEL,
        "fast_model": ai_admin.get_fast_moderation_model(),
        "automod_config": ai_admin.get_automod_config()
    }

@app.get("/api/host/ai/config")
async def host_ai_get_config(request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    return {"status": "ok", "config": ai_admin.get_automod_config()}

@app.post("/api/host/ai/config")
async def host_ai_set_config(request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    data = await request.json()
    enabled = data.get("enabled")
    mode = data.get("mode")
    model = data.get("model")
    purge = data.get("purge")
    ai_admin.set_automod_config(enabled=enabled, mode=mode, model=model, purge=purge)
    return {"status": "ok", "config": ai_admin.get_automod_config()}

@app.get("/api/host/ai/logs")
async def host_ai_logs(request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    logs = database.get_ai_moderation_logs(limit=100)
    return {"status": "ok", "logs": logs}

@app.post("/api/host/ai/logs/delete/{log_id}")
async def host_ai_log_delete(log_id: int, request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    success = database.delete_ai_moderation_log(log_id)
    return {"status": "ok", "deleted": success}

@app.post("/api/host/ai/logs/clear")
async def host_ai_logs_clear(request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    count = database.clear_ai_moderation_logs()
    return {"status": "ok", "cleared_count": count}


@app.post("/api/host/ai/chat")
async def host_ai_chat(request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    data = await request.json()
    prompt = str(data.get("prompt", "")).strip()
    if not prompt:
        raise HTTPException(status_code=400, detail="Prompt is required")
    
    history = data.get("history", [])
    model = data.get("model")
    include_context = bool(data.get("include_context", True))
    room_id = str(data.get("room_id", "main"))

    result = await ai_admin.chat_with_qwen(
        user_prompt=prompt,
        history=history,
        model=model,
        include_chat_context=include_context,
        room_id=room_id,
        manager=manager
    )
    return result

@app.post("/api/host/ai/scan")
async def host_ai_scan(request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    data = await request.json()
    room_id = str(data.get("room_id", "main"))
    model = data.get("model")
    auto_enforce = bool(data.get("auto_enforce", False))
    result = await ai_admin.scan_moderation(room_id=room_id, model=model, manager=manager, auto_enforce=auto_enforce)
    return result

@app.post("/api/host/ai/moderate_post/{post_num}")
async def host_ai_moderate_single_post(post_num: int, request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    p_info = database.get_post_info(post_num)
    if not p_info:
        raise HTTPException(status_code=404, detail=f"Post #{post_num} not found")
    event = await ai_admin.moderate_incoming_message(p_info, manager=manager)
    return {"status": "ok", "post_num": post_num, "moderated": bool(event), "event": event}


# --- Anonymous Feedback Endpoints ---
@app.post("/api/feedback")
async def submit_feedback(request: Request):
    client_ip = get_client_ip(request)
    session_id = request.cookies.get("session_id") or ""
    identity_id = database.get_or_create_identity_id(session_id)
    public_id = generate_public_id(identity_id)
    animal = animal_avatars.get_animal_for_public_id(public_id)
    
    data = await request.json()
    category = str(data.get("category", "General Suggestion")).strip()[:60]
    message = str(data.get("message", "")).strip()[:1500]
    try:
        rating = min(max(int(data.get("rating", 5)), 1), 5)
    except Exception:
        rating = 5
    
    if not message:
        raise HTTPException(status_code=400, detail="Feedback message cannot be empty")
    
    fid = database.insert_feedback(
        public_id=public_id,
        anon_name=animal["handle"],
        ip=client_ip,
        category=category,
        message=message,
        rating=rating
    )
    return {
        "status": "ok",
        "feedback_id": fid,
        "message": "Thank you! Your feedback has been anonymously submitted to the PESITM administration."
    }

@app.get("/api/host/feedbacks")
async def host_get_feedbacks(request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    feedbacks = database.get_feedbacks(limit=100)
    return {"status": "ok", "feedbacks": feedbacks}

@app.post("/api/host/feedbacks/delete/{feedback_id}")
async def host_delete_feedback(feedback_id: int, request: Request):
    client_ip = get_client_ip(request)
    if not is_host_client(client_ip, request.cookies, request=request):
        raise HTTPException(status_code=403, detail="Access restricted to host laptop")
    deleted = database.delete_feedback(feedback_id)
    return {"status": "ok" if deleted else "not_found", "id": feedback_id}

@app.get("/api/public_rooms")
async def get_public_rooms():
    rooms = database.get_all_public_rooms()
    return {
        "status": "ok",
        "rooms": rooms
    }

@app.get("/api/public_groups")
async def get_public_groups():
    groups = database.get_discoverable_public_groups()
    return {
        "status": "ok",
        "rooms": groups,
        "groups": groups
    }

@app.get("/api/discoverable_groups")
async def get_discoverable_groups():
    return await get_public_groups()

@app.get("/api/app/version")
@app.get("/api/version")
@app.get("/version.json")
async def get_app_version(request: Request):
    """
    Returns latest AnnoChat Android APK version metadata and download URL.
    Supports force update enforcement and dynamic URL resolution.
    """
    version_file = config.UPDATES_DIR / "version.json"
    data = None
    if version_file.exists():
        try:
            with open(version_file, "r", encoding="utf-8") as f:
                data = json.load(f)
        except Exception:
            data = None

    if not data:
        data = {
            "latestVersionCode": 3,
            "latestVersionName": "1.0.2",
            "apkUrl": "/download/app.apk",
            "releaseNotes": [
                "Compact streamlined bottom navigation bar",
                "Enhanced group list and messaging layouts",
                "Optimized and concise signed release build"
            ],
            "forceUpdate": False,
            "minimumSupportedVersionCode": 1
        }

    # Ensure full URL for apkUrl if provided as relative path
    apk_url = str(data.get("apkUrl", "")).strip()
    if apk_url.startswith("/"):
        base_url = str(request.base_url).rstrip("/")
        data["apkUrl"] = f"{base_url}{apk_url}"
    elif apk_url and not (apk_url.startswith("http://") or apk_url.startswith("https://")):
        base_url = str(request.base_url).rstrip("/")
        data["apkUrl"] = f"{base_url}/updates/{apk_url}"

    return JSONResponse(content=data)


@app.get("/download/app.apk")
async def download_app_apk():
    """Direct APK download route for mobile web and in-app updater."""
    apk_path = config.UPDATES_DIR / "AfterClass.apk"
    if not apk_path.exists():
        apk_path = config.UPDATES_DIR / "annochat-1.0.apk"
    if apk_path.exists():
        return FileResponse(
            path=str(apk_path),
            filename="AfterClass.apk",
            media_type="application/vnd.android.package-archive"
        )
    return JSONResponse(status_code=404, content={"detail": "APK not found"})



# Real-time WebSocket endpoint
@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    # Origin verification to prevent Cross-Site WebSocket Hijacking (CSWSH)
    origin = websocket.headers.get("origin")
    if origin:
        allowed = False
        try:
            p_origin = urllib.parse.urlparse(origin)
            orig_host = (p_origin.hostname or "").lower()

            # 1. Same-Origin check: Origin host matches request Host header
            host_header = (websocket.headers.get("host") or "").strip()
            if host_header:
                req_host = (
                    host_header.split("]")[0].lstrip("[")
                    if host_header.startswith("[")
                    else host_header.split(":")[0]
                ).lower()
                if orig_host and req_host and orig_host == req_host:
                    allowed = True

            # 2. Reverse proxy / tunnel: Origin host matches X-Forwarded-Host header
            if not allowed:
                fwd_header = (websocket.headers.get("x-forwarded-host") or "").split(",")[0].strip()
                if fwd_header:
                    fwd_host = (
                        fwd_header.split("]")[0].lstrip("[")
                        if fwd_header.startswith("[")
                        else fwd_header.split(":")[0]
                    ).lower()
                    if orig_host and fwd_host and orig_host == fwd_host:
                        allowed = True

            # 3. Local loopback / bind addresses
            if not allowed and orig_host in ("localhost", "127.0.0.1", "::1", "0.0.0.0", "127.0.1.1"):
                allowed = True

            # 4. Machine local hostname
            if not allowed:
                try:
                    local_hostname = socket.gethostname().lower()
                    if orig_host in (local_hostname, f"{local_hostname}.local"):
                        allowed = True
                except Exception:
                    pass

            # 5. Configured public tunnel / domain
            if not allowed:
                try:
                    pub_url = tunnel.get_public_url()
                    if pub_url:
                        pub_p = urllib.parse.urlparse(pub_url)
                        if pub_p.hostname and orig_host == pub_p.hostname.lower():
                            allowed = True
                except Exception:
                    pass

            # 6. Known tunnel domain suffixes
            if not allowed:
                allowed_suffixes = (
                    "localhost",
                    "trycloudflare.com",
                    "ngrok-free.app",
                    "ngrok.io",
                    "ngrok-free.dev",
                    "ngrok.app",
                    "ngrok.dev",
                    "afterclass.bond",
                )
                if any(orig_host == sfx or orig_host.endswith("." + sfx) for sfx in allowed_suffixes):
                    allowed = True

            # 7. LAN IP and private network addresses (RFC 1918 / CGNAT / local subnets)
            if not allowed:
                try:
                    lan_ip = network_intel.get_lan_ip()
                    if lan_ip and orig_host == lan_ip:
                        allowed = True
                except Exception:
                    pass

            if not allowed:
                try:
                    ip_obj = ipaddress.ip_address(orig_host)
                    if ip_obj.is_private or ip_obj.is_loopback:
                        allowed = True
                except ValueError:
                    pass

        except Exception:
            allowed = False

        if not allowed:
            await websocket.close(code=1008)
            return

    client_host = websocket.client.host if websocket.client else "127.0.0.1"
    client_ip = client_host
    is_tunnel = False

    if client_host in ("127.0.0.1", "::1"):
        cf_ip = websocket.headers.get("cf-connecting-ip")
        if cf_ip and network_intel.is_valid_ip(cf_ip.strip()):
            client_ip = cf_ip.strip()
            is_tunnel = True
        elif websocket.headers.get("cf-ray") or websocket.headers.get("ngrok-trace-id"):
            is_tunnel = True
        else:
            for header_name, header_val in websocket.headers.items():
                if header_name.lower() == "x-forwarded-for":
                    first_ip = header_val.split(",")[0].strip()
                    if network_intel.is_valid_ip(first_ip):
                        client_ip = first_ip
                        is_tunnel = True
                        break

    # Check if request came through Cloudflare or ngrok Tunnel
    host_hdr = websocket.headers.get("host") or ""
    if (
        websocket.headers.get("cf-connecting-ip")
        or websocket.headers.get("cf-ray")
        or websocket.headers.get("ngrok-trace-id")
        or "ngrok" in host_hdr
        or "trycloudflare" in host_hdr
        or "afterclass.bond" in host_hdr
        or (origin and ("trycloudflare.com" in origin or "ngrok" in origin or "afterclass.bond" in origin))
    ):
        is_tunnel = True

    if not network_intel.is_valid_ip(client_ip):
        client_ip = "127.0.0.1"

    # Host verification: Requires valid host token cookie or query param (never auto-granted over WS or loopback)
    is_host = False
    if websocket.cookies.get("host_logged_out") != "1":
        verified_token = websocket.cookies.get("host_verified") or websocket.query_params.get("admin_token")
        if verified_token and hmac.compare_digest(str(verified_token), str(config.ADMIN_TOKEN_SECRET)):
            is_host = True



    # Check connection capacity limits for 500+ users
    can_conn, reason = manager.can_connect(client_ip)
    if not can_conn:
        await websocket.accept()
        await websocket.send_text(json.dumps({"type": "error", "message": reason}))
        await websocket.close()
        return

    if database.is_ip_banned(client_ip):
        await websocket.accept()
        await websocket.send_text(json.dumps({"type": "error", "message": "You are currently blocked by the host."}))
        await websocket.close()
        return

    raw_sess = websocket.query_params.get("session_id") or websocket.cookies.get("session_id")
    if raw_sess and len(raw_sess) <= 64 and re.match(r'^[a-zA-Z0-9_-]+$', raw_sess):
        session_id = raw_sess
    else:
        session_id = uuid.uuid4().hex[:12]

    identity_id = database.get_or_create_identity_id(session_id)
    public_id = generate_public_id(identity_id)
    
    # Assign animal identity
    animal = animal_avatars.get_animal_for_public_id(public_id)

    hostname = await asyncio.to_thread(network_intel.resolve_hostname_fast, client_ip)
    mac = await asyncio.to_thread(network_intel.get_mac_for_ip, client_ip)
    intel = network_intel.parse_device_intel(websocket.headers.get("user-agent", ""))
    device_summary = intel["summary"]

    await asyncio.to_thread(database.upsert_identity, ip=client_ip, hostname=hostname, mac=mac, device_summary=device_summary)
    await manager.connect(websocket, client_ip, session_id, public_id, animal["handle"], animal["avatar_url"], animal.get("sci_name", ""), is_host, websocket.headers.get("user-agent", ""))

    def send_self(payload: dict):
        manager.send_personal(websocket, payload)

    # Send client their assigned animal identity safely via bounded write queue
    send_self({
        "type": "my_identity",
        "anon_name": animal["handle"],
        "animal_name": animal["name"],
        "sci_name": animal["sci_name"],
        "avatar_url": animal["avatar_url"],
        "public_id": public_id,
        "is_host": is_host
    })

    last_post_time = 0.0
    last_typing_time = 0.0
    recent_reactions: List[float] = []

    try:
        while True:
            data = await websocket.receive_text()
            # Guard against giant memory-exhausting WebSocket payloads
            if len(data) > 65536:
                continue

            try:
                payload = json.loads(data)
            except Exception:
                continue

            action = payload.get("action")
            now = time.time()
            if websocket in manager.active_connections:
                manager.active_connections[websocket]["last_seen"] = now

            if action == "ping":
                send_self({"type": "pong", "time": payload.get("time")})

            elif action == "authenticate_admin":
                tok = payload.get("token") or ""
                if tok and hmac.compare_digest(str(tok), str(config.ADMIN_TOKEN_SECRET)):
                    is_host = True
                    if websocket in manager.active_connections:
                        manager.active_connections[websocket]["is_host"] = True
                    manager.host_sockets.add(websocket)
                    send_self({"type": "admin_authenticated", "status": "ok"})
                else:
                    send_self({"type": "error", "message": "Invalid admin token"})
                continue

            elif action == "admin_logout":
                is_host = False
                if websocket in manager.active_connections:
                    manager.active_connections[websocket]["is_host"] = False
                manager.host_sockets.discard(websocket)
                send_self({"type": "admin_logged_out", "status": "ok"})
                continue

            elif action == "typing":
                # Rate limit typing broadcasts
                if now - last_typing_time < 2.5:
                    continue
                last_typing_time = now
                room_id = payload.get("room_id")
                await manager.broadcast_typing(
                    animal["name"],
                    websocket,
                    public_id=public_id,
                    session_id=session_id,
                    room_id=room_id
                )

            elif action == "react":
                # Throttled reaction spam prevention
                recent_reactions = [t for t in recent_reactions if now - t < 2.0]
                if len(recent_reactions) >= 6:
                    continue
                recent_reactions.append(now)

                post_num = payload.get("post_num")
                emoji = payload.get("emoji")
                if post_num and emoji and len(emoji) <= 8:
                    try:
                        p_num = int(post_num)
                        reactions = database.toggle_reaction(p_num, emoji, session_id)
                        await manager.broadcast_reaction(p_num, reactions)
                    except Exception:
                        pass


            elif action == "request_private_chat":
                target_public_id = payload.get("target_public_id")
                if not target_public_id or target_public_id == public_id:
                    send_self({"type": "error", "message": "Cannot chat with yourself."})
                    continue

                target_found = False
                target_name = "Anonymous"
                for ws_t, info_t in manager.active_connections.items():
                    if info_t.get("public_id") == target_public_id:
                        target_found = True
                        target_name = info_t.get("anon_name", "Anonymous")
                        break

                if not target_found:
                    send_self({"type": "error", "message": "User is currently offline."})
                    continue

                invite_id = uuid.uuid4().hex[:8]
                target_room_id = payload.get("room_id")
                target_room_name = payload.get("room_name")
                manager.pending_invites[invite_id] = {
                    "from_id": public_id,
                    "from_name": animal["handle"],
                    "from_avatar": animal["avatar_url"],
                    "to_id": target_public_id,
                    "room_id": target_room_id,
                    "room_name": target_room_name,
                    "is_group": bool(target_room_id),
                    "created_at": time.time()
                }

                # Send invite to target
                await manager.send_to_user(target_public_id, {
                    "type": "private_chat_invite",
                    "invite_id": invite_id,
                    "from_id": public_id,
                    "from_name": animal["handle"],
                    "from_avatar": animal["avatar_url"],
                    "room_id": target_room_id,
                    "room_name": target_room_name,
                    "is_group": bool(target_room_id)
                })

                send_self({
                    "type": "private_chat_sent",
                    "target_name": target_name,
                    "is_group": bool(target_room_id),
                    "room_name": target_room_name
                })

            elif action == "respond_private_chat":
                invite_id = payload.get("invite_id")
                accepted = bool(payload.get("accepted"))
                invite = manager.pending_invites.pop(invite_id, None)
                if not invite:
                    send_self({"type": "error", "message": "Invite expired or already handled."})
                    continue

                sender_id = invite["from_id"]
                sender_name = invite["from_name"]
                sender_avatar = invite["from_avatar"]

                if not accepted:
                    await manager.send_to_user(sender_id, {
                        "type": "private_chat_declined",
                        "declined_by": animal["handle"],
                        "room_id": invite.get("room_id"),
                        "is_group": bool(invite.get("room_id"))
                    })
                    continue

                if invite.get("room_id"):
                    # Joining an EXISTING private group room
                    target_room_id = invite["room_id"]
                    await asyncio.to_thread(database.add_user_to_room, target_room_id, public_id, animal["handle"], animal["avatar_url"])
                    manager.join_room(websocket, target_room_id)
                    manager.join_user_to_room(sender_id, target_room_id)

                    sys_msg = await asyncio.to_thread(
                        database.insert_message,
                        session_id="system",
                        public_id="system",
                        anon_name="SYSTEM",
                        anon_avatar="/api/animal/snow_leopard",
                        ip="127.0.0.1",
                        hostname="localhost",
                        mac="",
                        user_agent="",
                        device_summary="",
                        content=f"{animal['handle']} accepted the invite and joined the chat.",
                        room_id=target_room_id
                    )

                    members = await asyncio.to_thread(database.get_room_members, target_room_id)
                    conn = database.get_db_connection()
                    cur = conn.cursor()
                    cur.execute("SELECT name FROM rooms WHERE room_id = ?", (target_room_id,))
                    r_row = cur.fetchone()
                    conn.close()
                    room_title = invite.get("room_name") or (r_row["name"] if r_row else f"Group ({len(members)} users)")

                    room_info = {
                        "room_id": target_room_id,
                        "name": room_title,
                        "members": members
                    }

                    # Send room joined to the user who accepted
                    await manager.send_to_user(public_id, {
                        "type": "room_joined",
                        "room": room_info,
                        "system_message": sys_msg
                    })

                    # Send direct acceptance notification to the inviter
                    await manager.send_to_user(sender_id, {
                        "type": "room_invitation_accepted",
                        "accepted_by": animal["handle"],
                        "room_id": target_room_id,
                        "room_name": room_title
                    })

                    # Broadcast update to room members
                    await manager.broadcast_to_room(target_room_id, {
                        "type": "room_updated",
                        "room": room_info,
                        "system_message": sys_msg
                    })

                    await manager.broadcast_message(sys_msg)

                else:
                    # Creating a NEW 1-on-1 private room
                    room_id = f"dm_{uuid.uuid4().hex[:8]}"
                    room_name = f"{sender_name} & {animal['handle']}"
                    members = [
                        {"public_id": sender_id, "anon_name": sender_name, "anon_avatar": sender_avatar},
                        {"public_id": public_id, "anon_name": animal["handle"], "anon_avatar": animal["avatar_url"]}
                    ]
                    room_data = await asyncio.to_thread(database.create_private_room, room_id, room_name, sender_id, members)
                    manager.join_user_to_room(sender_id, room_id)
                    manager.join_room(websocket, room_id)

                    sys_msg = await asyncio.to_thread(
                        database.insert_message,
                        session_id="system",
                        public_id="system",
                        anon_name="SYSTEM",
                        anon_avatar="/api/animal/snow_leopard",
                        ip="127.0.0.1",
                        hostname="localhost",
                        mac="",
                        user_agent="",
                        device_summary="",
                        content=f"Private room created between {sender_name} and {animal['handle']}. Only participants can view these messages.",
                        room_id=room_id
                    )

                    room_event = {
                        "type": "room_joined",
                        "room": room_data,
                        "system_message": sys_msg
                    }

                    await manager.send_to_user(sender_id, room_event)
                    await manager.send_to_user(public_id, room_event)

                    # Inform host so host can see and moderate any created room
                    host_event = {
                        "type": "room_created",
                        "room": room_data,
                        "system_message": sys_msg
                    }
                    for ws_h, info_h in manager.active_connections.items():
                        if info_h.get("is_host") and info_h.get("public_id") not in (sender_id, public_id):
                            manager.send_personal(ws_h, host_event)

            elif action in ("add_user_to_room", "admin_add_user_to_room"):
                target_public_id = payload.get("target_public_id")
                target_room_id = payload.get("room_id")
                if not target_room_id or target_room_id == "main" or not target_public_id:
                    send_self({"type": "error", "message": "Invalid room or user."})
                    continue

                is_member = (
                    database.is_user_in_room(target_room_id, public_id, is_host=False)
                    or websocket in manager.room_members.get(target_room_id, set())
                    or target_room_id.startswith("grp_")
                    or target_room_id.startswith("dm_")
                )
                if not is_host and not is_member:
                    send_self({"type": "error", "message": "You must be a member of this chat to add others."})
                    continue

                if not database.is_user_in_room(target_room_id, public_id, is_host=False):
                    await asyncio.to_thread(database.add_user_to_room, target_room_id, public_id, animal["handle"], animal["avatar_url"])
                manager.join_room(websocket, target_room_id)

                # Look up target user info
                target_user = None
                for ws_t, info_t in manager.active_connections.items():
                    if info_t.get("public_id") == target_public_id:
                        target_user = info_t
                        break

                if not target_user:
                    send_self({"type": "error", "message": "User is currently offline."})
                    continue

                target_name = target_user.get("anon_name", "Anonymous")

                # Get room name
                conn = database.get_db_connection()
                cur = conn.cursor()
                cur.execute("SELECT name FROM rooms WHERE room_id = ?", (target_room_id,))
                r_row = cur.fetchone()
                conn.close()
                room_title = payload.get("room_name") or (r_row["name"] if r_row else "Private Chat")

                # Send invitation request to target user (must accept to join)
                invite_id = uuid.uuid4().hex[:8]
                manager.pending_invites[invite_id] = {
                    "from_id": public_id,
                    "from_name": animal["handle"],
                    "from_avatar": animal["avatar_url"],
                    "to_id": target_public_id,
                    "room_id": target_room_id,
                    "room_name": room_title,
                    "is_group": True,
                    "created_at": time.time()
                }

                await manager.send_to_user(target_public_id, {
                    "type": "private_chat_invite",
                    "invite_id": invite_id,
                    "from_id": public_id,
                    "from_name": animal["handle"],
                    "from_avatar": animal["avatar_url"],
                    "room_id": target_room_id,
                    "room_name": room_title,
                    "is_group": True
                })

                send_self({
                    "type": "private_chat_sent",
                    "target_name": target_name,
                    "is_group": True,
                    "room_name": room_title
                })

            elif action == "leave_room":
                target_room_id = payload.get("room_id")
                if not target_room_id or target_room_id == "main":
                    continue

                manager.leave_room(websocket, target_room_id)
                was_deleted = await asyncio.to_thread(database.remove_user_from_room, target_room_id, public_id)

                # Send confirmation to leaving user
                send_self({
                    "type": "room_left",
                    "room_id": target_room_id
                })

                if was_deleted:
                    await manager.broadcast_to_room(target_room_id, {
                        "type": "room_deleted",
                        "room_id": target_room_id,
                        "reason": "All members left the group"
                    })
                else:
                    sys_msg = await asyncio.to_thread(
                        database.insert_message,
                        session_id="system",
                        public_id="system",
                        anon_name="SYSTEM",
                        anon_avatar="/api/animal/snow_leopard",
                        ip="127.0.0.1",
                        hostname="localhost",
                        mac="",
                        user_agent="",
                        device_summary="",
                        content=f"{animal['handle']} left the chat.",
                        room_id=target_room_id
                    )

                    remaining_members = await asyncio.to_thread(database.get_room_members, target_room_id)
                    conn = database.get_db_connection()
                    cur = conn.cursor()
                    cur.execute("SELECT name FROM rooms WHERE room_id = ?", (target_room_id,))
                    r_row = cur.fetchone()
                    conn.close()
                    room_title = r_row["name"] if r_row else "Private Chat"

                    room_info = {
                        "room_id": target_room_id,
                        "name": room_title,
                        "members": remaining_members
                    }

                    await manager.broadcast_to_room(target_room_id, {
                        "type": "room_updated",
                        "room": room_info,
                        "system_message": sys_msg
                    })

                    await manager.broadcast_message(sys_msg)

            elif action == "join_room":
                target_room_id = payload.get("room_id")
                if target_room_id:
                    clean_r = re.sub(r'[^a-zA-Z0-9_-]', '', str(target_room_id))[:50] or "main"
                    if clean_r in database.PUBLIC_ROOMS or is_host or database.is_user_in_room(clean_r, public_id, is_host):
                        manager.join_room(websocket, clean_r)
                    else:
                        room_info = await asyncio.to_thread(database.get_room_info, clean_r)
                        if room_info and bool(room_info.get("is_public")):
                            animal = animal_avatars.get_animal_for_public_id(public_id)
                            await asyncio.to_thread(database.add_user_to_room, clean_r, public_id, animal["handle"], animal["avatar_url"])
                            manager.join_room(websocket, clean_r)

            elif action == "post_message":
                if database.is_ip_banned(client_ip):
                    send_self({"type": "error", "message": "You are banned."})
                    break

                if database.is_ip_muted(client_ip):
                    send_self({"type": "error", "message": "You are currently muted by the host."})
                    continue

                room_id = payload.get("room_id") or "main"
                clean_room = re.sub(r'[^a-zA-Z0-9_-]', '', str(room_id))[:50] or "main"
                if clean_room not in database.PUBLIC_ROOMS and not is_host:
                    if not database.is_user_in_room(clean_room, public_id, is_host):
                        room_info = await asyncio.to_thread(database.get_room_info, clean_room)
                        if room_info and bool(room_info.get("is_public")):
                            animal = animal_avatars.get_animal_for_public_id(public_id)
                            await asyncio.to_thread(database.add_user_to_room, clean_room, public_id, animal["handle"], animal["avatar_url"])
                            manager.join_user_to_room(public_id, clean_room)
                        else:
                            send_self({"type": "error", "message": "You are not a participant in this room."})
                            continue

                client_msg_id = payload.get("client_msg_id")
                if client_msg_id:
                    client_msg_id = str(client_msg_id).strip()[:100] or None

                # Fast idempotency deduplication: If client_msg_id was already processed for this session and room, drop duplicate immediately
                if client_msg_id:
                    existing_msg = await asyncio.to_thread(
                        database.get_message_by_client_msg_id,
                        session_id=session_id,
                        room_id=clean_room,
                        client_msg_id=client_msg_id
                    )
                    if existing_msg:
                        # Duplicate client_msg_id already processed: ignore retry, do not broadcast
                        continue

                # Rate limit message posting (prevents flooding 500 connected users)
                if now - last_post_time < config.RATE_LIMIT_POST_SEC:
                    send_self({"type": "error", "message": "Slow down! Wait a second before posting again."})
                    continue
                last_post_time = now

                content = (payload.get("content") or "").strip()
                # Enforce message character limit
                if len(content) > config.MAX_MESSAGE_LENGTH:
                    content = content[:config.MAX_MESSAGE_LENGTH]

                def is_valid_media_path(p):
                    if not p or not isinstance(p, str):
                        return False
                    if ".." in p or "\\" in p or "\x00" in p:
                        return False
                    if p.startswith("/uploads/") or p.startswith("/static/stickers/") or p.startswith("/static/gifs/"):
                        return True
                    if p.startswith("http://") or p.startswith("https://"):
                        return True
                    if p.startswith("sticker_") or p.startswith("gif_"):
                        return True
                    return False

                image_path = payload.get("image_path")

                # Auto-extract GIF/sticker/media URL if user sent it or Gboard pasted it as content
                if not image_path and content:
                    extracted_media = extract_media_url_from_text(content)
                    if extracted_media:
                        image_path = extracted_media
                        # Remove the media URL from content so it does not show as a text URL!
                        content = content.replace(extracted_media, "").strip()

                if not is_valid_media_path(image_path):
                    image_path = None

                if image_path:
                    # Resolve Tenor/Giphy page links to direct playable media URL in worker thread
                    image_path = await asyncio.to_thread(resolve_media_to_direct_url, image_path)
                    if not is_valid_media_path(image_path):
                        image_path = None

                image_thumb = payload.get("image_thumb")
                if not is_valid_media_path(image_thumb):
                    image_thumb = image_path if image_path else None
                elif image_thumb and ("tenor.com/view/" in image_thumb or "giphy.com/gifs/" in image_thumb):
                    image_thumb = image_path

                image_name = payload.get("image_name")
                if image_name:
                    image_name = re.sub(r'[^a-zA-Z0-9._ -]', '', str(image_name))[:60]
                elif image_path:
                    lower_p = image_path.lower()
                    if "sticker" in lower_p or lower_p.endswith(".svg"):
                        image_name = "Sticker"
                    elif "gif" in lower_p or "tenor" in lower_p or "giphy" in lower_p:
                        image_name = "GIF"
                    else:
                        image_name = "Media"

                image_size = payload.get("image_size")
                image_dims = payload.get("image_dims")
                
                reply_to = payload.get("reply_to")
                if reply_to is not None:
                    try:
                        reply_to = int(reply_to)
                    except (TypeError, ValueError):
                        reply_to = None

                if not content and not image_path:
                    continue

                new_msg = await asyncio.to_thread(
                    database.insert_message,
                    session_id=session_id,
                    public_id=public_id,
                    anon_name=animal["handle"],
                    anon_avatar=animal["avatar_url"],
                    ip=client_ip,
                    hostname=hostname,
                    mac=mac,
                    user_agent=websocket.headers.get("user-agent", ""),
                    device_summary=device_summary,
                    content=content,
                    image_path=image_path,
                    image_thumb=image_thumb,
                    image_name=image_name,
                    image_size=image_size,
                    image_dims=image_dims,
                    reply_to=reply_to,
                    room_id=clean_room,
                    client_msg_id=client_msg_id
                )

                if new_msg.get("_is_duplicate"):
                    # Duplicate client_msg_id detected by database constraint: do not broadcast or duplicate
                    continue

                if clean_room not in database.PUBLIC_ROOMS and not is_host:
                    await asyncio.to_thread(database.add_user_to_room, clean_room, public_id, animal["handle"], animal["avatar_url"])
                manager.join_room(websocket, clean_room)

                await manager.broadcast_message(new_msg)

                # Autonomous Real-Time AI Auto-Moderator Hook:
                # Evaluates incoming post in real time to delete offending posts and ban authors instantly
                asyncio.create_task(ai_admin.moderate_incoming_message(new_msg, manager=manager))

    except WebSocketDisconnect:
        manager.disconnect(websocket)
        remaining_conns = [ws for ws, info in manager.active_connections.items() if info.get("public_id") == public_id]
        if not remaining_conns and not is_host:
            deleted_rooms = await asyncio.to_thread(database.remove_user_from_all_rooms, public_id)
            for r_id in deleted_rooms:
                await manager.broadcast_to_room(r_id, {
                    "type": "room_deleted",
                    "room_id": r_id,
                    "message": "This group has been deleted because all members left."
                })
    except Exception:
        manager.disconnect(websocket)
        remaining_conns = [ws for ws, info in manager.active_connections.items() if info.get("public_id") == public_id]
        if not remaining_conns and not is_host:
            deleted_rooms = await asyncio.to_thread(database.remove_user_from_all_rooms, public_id)
            for r_id in deleted_rooms:
                await manager.broadcast_to_room(r_id, {
                    "type": "room_deleted",
                    "room_id": r_id,
                    "message": "This group has been deleted because all members left."
                })

