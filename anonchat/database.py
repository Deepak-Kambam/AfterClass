import sqlite3
import json
from datetime import datetime
from typing import List, Dict, Optional, Any, Tuple
from config import DB_PATH

def get_db_connection() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH, timeout=30.0, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA busy_timeout = 30000;")
    conn.execute("PRAGMA synchronous = NORMAL;")
    return conn


def init_db():
    conn = get_db_connection()
    cur = conn.cursor()

    cur.execute("""
    CREATE TABLE IF NOT EXISTS identities (
        ip TEXT PRIMARY KEY,
        real_name TEXT,
        notes TEXT,
        is_banned INTEGER DEFAULT 0,
        first_seen TEXT,
        last_seen TEXT,
        device_summary TEXT,
        hostname TEXT,
        mac TEXT
    );
    """)

    cur.execute("""
    CREATE TABLE IF NOT EXISTS messages (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        post_num INTEGER UNIQUE,
        session_id TEXT,
        public_id TEXT,
        anon_name TEXT,
        anon_avatar TEXT,
        ip TEXT,
        hostname TEXT,
        mac TEXT,
        user_agent TEXT,
        device_summary TEXT,
        content TEXT,
        image_path TEXT,
        image_thumb TEXT,
        image_name TEXT,
        image_size INTEGER,
        image_dims TEXT,
        reply_to INTEGER,
        created_at TEXT,
        is_deleted INTEGER DEFAULT 0
    );
    """)

    cur.execute("""
    CREATE TABLE IF NOT EXISTS reactions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        post_num INTEGER,
        emoji TEXT,
        session_id TEXT,
        created_at TEXT,
        UNIQUE(post_num, emoji, session_id)
    );
    """)

    cur.execute("""
    CREATE TABLE IF NOT EXISTS rooms (
        room_id TEXT PRIMARY KEY,
        name TEXT,
        desc TEXT DEFAULT '',
        created_by TEXT,
        created_at TEXT,
        is_active INTEGER DEFAULT 1,
        is_public INTEGER DEFAULT 1,
        owner_public_id TEXT DEFAULT ''
    );
    """)

    cur.execute("""
    CREATE TABLE IF NOT EXISTS room_members (
        room_id TEXT,
        public_id TEXT,
        anon_name TEXT,
        anon_avatar TEXT,
        joined_at TEXT,
        role TEXT DEFAULT 'MEMBER',
        PRIMARY KEY(room_id, public_id)
    );
    """)

    # Migration checks
    cur.execute("PRAGMA table_info(messages)")
    cols_m = [row["name"] for row in cur.fetchall()]
    if "anon_name" not in cols_m:
        cur.execute("ALTER TABLE messages ADD COLUMN anon_name TEXT;")
    if "anon_avatar" not in cols_m:
        cur.execute("ALTER TABLE messages ADD COLUMN anon_avatar TEXT;")
    if "is_pinned" not in cols_m:
        cur.execute("ALTER TABLE messages ADD COLUMN is_pinned INTEGER DEFAULT 0;")
    if "room_id" not in cols_m:
        cur.execute("ALTER TABLE messages ADD COLUMN room_id TEXT DEFAULT 'main';")
    if "client_msg_id" not in cols_m:
        cur.execute("ALTER TABLE messages ADD COLUMN client_msg_id TEXT;")
        cur.execute("CREATE INDEX IF NOT EXISTS idx_messages_client_msg ON messages(session_id, room_id, client_msg_id);")

    cur.execute("PRAGMA table_info(identities)")
    cols_i = [row["name"] for row in cur.fetchall()]
    if "is_muted" not in cols_i:
        cur.execute("ALTER TABLE identities ADD COLUMN is_muted INTEGER DEFAULT 0;")
    if "muted_until" not in cols_i:
        cur.execute("ALTER TABLE identities ADD COLUMN muted_until TEXT;")

    # Migration: add is_public, desc, owner_public_id to existing rooms tables
    cur.execute("PRAGMA table_info(rooms)")
    cols_r = [row["name"] for row in cur.fetchall()]
    if "is_public" not in cols_r:
        cur.execute("ALTER TABLE rooms ADD COLUMN is_public INTEGER DEFAULT 1;")
    if "desc" not in cols_r:
        cur.execute("ALTER TABLE rooms ADD COLUMN desc TEXT DEFAULT '';")
    if "owner_public_id" not in cols_r:
        cur.execute("ALTER TABLE rooms ADD COLUMN owner_public_id TEXT DEFAULT '';")

    # Migration: add role to existing room_members tables
    cur.execute("PRAGMA table_info(room_members)")
    cols_rm = [row["name"] for row in cur.fetchall()]
    if "role" not in cols_rm:
        cur.execute("ALTER TABLE room_members ADD COLUMN role TEXT DEFAULT 'MEMBER';")

    cur.execute("PRAGMA journal_mode=WAL;")
    cur.execute("PRAGMA synchronous=NORMAL;")
    cur.execute("PRAGMA cache_size=-64000;")
    cur.execute("PRAGMA temp_store=MEMORY;")

    cur.execute("""
    CREATE TABLE IF NOT EXISTS feedbacks (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        public_id TEXT,
        anon_name TEXT,
        ip TEXT,
        category TEXT,
        message TEXT,
        rating INTEGER DEFAULT 5,
        created_at TEXT
    );
    """)

    cur.execute("""
    CREATE TABLE IF NOT EXISTS ai_moderation_logs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        post_num INTEGER,
        anon_name TEXT,
        ip TEXT,
        action TEXT,
        reason TEXT,
        content TEXT,
        created_at TEXT
    );
    """)

    cur.execute("PRAGMA table_info(ai_moderation_logs)")
    cols_ai = [row["name"] for row in cur.fetchall()]
    if "content" not in cols_ai:
        cur.execute("ALTER TABLE ai_moderation_logs ADD COLUMN content TEXT;")


    cur.execute("""
    CREATE TABLE IF NOT EXISTS server_settings (
        key TEXT PRIMARY KEY,
        value TEXT
    );
    """)

    # High-performance indexes for 500 concurrent users
    cur.execute("CREATE INDEX IF NOT EXISTS idx_messages_room_post ON messages(room_id, is_deleted, post_num DESC);")
    cur.execute("CREATE INDEX IF NOT EXISTS idx_messages_ip ON messages(ip);")
    cur.execute("CREATE INDEX IF NOT EXISTS idx_feedbacks_time ON feedbacks(created_at DESC);")
    cur.execute("CREATE INDEX IF NOT EXISTS idx_ai_mod_time ON ai_moderation_logs(id DESC);")
    cur.execute("CREATE INDEX IF NOT EXISTS idx_room_members_lookup ON room_members(room_id, public_id);")
    cur.execute("CREATE INDEX IF NOT EXISTS idx_identities_ip ON identities(ip);")

    # Populate default public campus channels if not present
    for p_id, p_name in PUBLIC_ROOMS.items():
        cur.execute("SELECT 1 FROM rooms WHERE room_id = ?", (p_id,))
        if not cur.fetchone():
            cur.execute("""
                INSERT INTO rooms (room_id, name, desc, created_by, created_at, is_active, is_public)
                VALUES (?, ?, ?, 'system', datetime('now'), 1, 1)
            """, (p_id, p_name, f"Campus stream for #{p_name}"))

    conn.commit()
    conn.close()


def get_next_post_num() -> int:
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT MAX(post_num) FROM messages")
    row = cur.fetchone()
    conn.close()
    max_num = row[0] if row and row[0] is not None else 1000
    return max_num + 1

def upsert_identity(ip: str, hostname: str = "", mac: str = "", device_summary: str = ""):
    now = datetime.utcnow().isoformat()
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT ip, real_name FROM identities WHERE ip = ?", (ip,))
    row = cur.fetchone()
    if row:
        cur.execute("""
            UPDATE identities 
            SET last_seen = ?,
                hostname = CASE WHEN ? != '' THEN ? ELSE hostname END,
                mac = CASE WHEN ? != '' THEN ? ELSE mac END,
                device_summary = CASE WHEN ? != '' THEN ? ELSE device_summary END
            WHERE ip = ?
        """, (now, hostname, hostname, mac, mac, device_summary, device_summary, ip))
    else:
        cur.execute("""
            INSERT INTO identities (ip, real_name, notes, is_banned, first_seen, last_seen, device_summary, hostname, mac)
            VALUES (?, ?, ?, 0, ?, ?, ?, ?, ?)
        """, (ip, "", "", now, now, device_summary, hostname, mac))
    conn.commit()
    conn.close()

def get_identity(ip: str) -> Optional[Dict[str, Any]]:
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT * FROM identities WHERE ip = ?", (ip,))
    row = cur.fetchone()
    conn.close()
    return dict(row) if row else None

def is_ip_banned(ip: str) -> bool:
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT is_banned FROM identities WHERE ip = ?", (ip,))
    row = cur.fetchone()
    conn.close()
    return bool(row and row["is_banned"] == 1)

def assign_real_name(ip: str, real_name: str, notes: Optional[str] = None):
    now = datetime.utcnow().isoformat()
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT ip FROM identities WHERE ip = ?", (ip,))
    if cur.fetchone():
        if notes is not None:
            cur.execute("UPDATE identities SET real_name = ?, notes = ?, last_seen = ? WHERE ip = ?", (real_name, notes, now, ip))
        else:
            cur.execute("UPDATE identities SET real_name = ?, last_seen = ? WHERE ip = ?", (real_name, now, ip))
    else:
        cur.execute("INSERT INTO identities (ip, real_name, notes, first_seen, last_seen) VALUES (?, ?, ?, ?, ?)",
                    (ip, real_name, notes or "", now, now))
    conn.commit()
    conn.close()

def set_ban_status(ip: str, banned: bool):
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT ip FROM identities WHERE ip = ?", (ip,))
    if cur.fetchone():
        cur.execute("UPDATE identities SET is_banned = ? WHERE ip = ?", (1 if banned else 0, ip))
    else:
        now = datetime.utcnow().isoformat()
        cur.execute("INSERT INTO identities (ip, is_banned, first_seen, last_seen) VALUES (?, ?, ?, ?)",
                    (ip, 1 if banned else 0, now, now))
    conn.commit()
    conn.close()

def set_mute_status(ip: str, muted: bool, minutes: Optional[int] = None):
    now = datetime.utcnow()
    muted_until = None
    if muted and minutes:
        from datetime import timedelta
        muted_until = (now + timedelta(minutes=minutes)).isoformat()

    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT ip FROM identities WHERE ip = ?", (ip,))
    if cur.fetchone():
        cur.execute("UPDATE identities SET is_muted = ?, muted_until = ? WHERE ip = ?", 
                    (1 if muted else 0, muted_until, ip))
    else:
        cur.execute("INSERT INTO identities (ip, is_muted, muted_until, first_seen, last_seen) VALUES (?, ?, ?, ?, ?)",
                    (ip, 1 if muted else 0, muted_until, now.isoformat(), now.isoformat()))
    conn.commit()
    conn.close()

def is_ip_muted(ip: str) -> bool:
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT is_muted, muted_until FROM identities WHERE ip = ?", (ip,))
    row = cur.fetchone()
    conn.close()
    if not row or not row["is_muted"]:
        return False
    # If muted_until is set, check if still in the future
    if row["muted_until"]:
        try:
            until = datetime.fromisoformat(row["muted_until"])
            if datetime.utcnow() > until:
                return False
        except Exception:
            pass
    return True

def get_message_by_client_msg_id(session_id: str, room_id: str, client_msg_id: str) -> Optional[Dict[str, Any]]:
    if not client_msg_id:
        return None
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("""
        SELECT * FROM messages 
        WHERE session_id = ? AND room_id = ? AND client_msg_id = ? AND is_deleted = 0
        ORDER BY id DESC LIMIT 1
    """, (session_id, room_id, client_msg_id))
    row = cur.fetchone()
    conn.close()
    return dict(row) if row else None

def insert_message(
    session_id: str,
    public_id: str,
    anon_name: str,
    anon_avatar: str,
    ip: str,
    hostname: str,
    mac: str,
    user_agent: str,
    device_summary: str,
    content: str,
    image_path: Optional[str] = None,
    image_thumb: Optional[str] = None,
    image_name: Optional[str] = None,
    image_size: Optional[int] = None,
    image_dims: Optional[str] = None,
    reply_to: Optional[int] = None,
    room_id: str = "main",
    client_msg_id: Optional[str] = None
) -> Dict[str, Any]:
    if client_msg_id:
        existing = get_message_by_client_msg_id(session_id, room_id, client_msg_id)
        if existing:
            existing["_is_duplicate"] = True
            return existing

    created_at = datetime.utcnow().isoformat()

    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("""
        INSERT INTO messages (
            post_num, session_id, public_id, anon_name, anon_avatar, ip, hostname, mac, user_agent,
            device_summary, content, image_path, image_thumb, image_name,
            image_size, image_dims, reply_to, room_id, client_msg_id, created_at, is_deleted
        ) VALUES (
            COALESCE((SELECT MAX(post_num) FROM messages), 1000) + 1,
            ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0
        )
    """, (
        session_id, public_id, anon_name, anon_avatar, ip, hostname, mac, user_agent,
        device_summary, content, image_path, image_thumb, image_name,
        image_size, image_dims, reply_to, room_id, client_msg_id, created_at
    ))
    conn.commit()
    msg_id = cur.lastrowid
    cur.execute("SELECT post_num FROM messages WHERE id = ?", (msg_id,))
    p_row = cur.fetchone()
    post_num = p_row["post_num"] if p_row else 1001

    # Retrieve real_name for identity
    cur.execute("SELECT real_name, notes, is_banned FROM identities WHERE ip = ?", (ip,))
    ident = cur.fetchone()

    reply_name = ""
    reply_snippet = ""
    if reply_to:
        cur.execute("SELECT anon_name, content, image_name, image_path FROM messages WHERE post_num = ?", (reply_to,))
        rm = cur.fetchone()
        if rm:
            reply_name = rm["anon_name"] or "Anonymous"
            if rm["content"]:
                reply_snippet = rm["content"][:100]
            elif rm["image_name"]:
                reply_snippet = rm["image_name"]
            elif rm["image_path"]:
                p = str(rm["image_path"]).lower()
                if "sticker" in p or p.endswith(".svg"):
                    reply_snippet = "Sticker"
                elif "gif" in p or "tenor" in p or "giphy" in p:
                    reply_snippet = "GIF"
                else:
                    reply_snippet = "Photo"

    conn.close()

    real_name = ident["real_name"] if ident and ident["real_name"] else ""
    notes = ident["notes"] if ident and ident["notes"] else ""
    is_banned = bool(ident and ident["is_banned"] == 1)

    return {
        "id": msg_id,
        "post_num": post_num,
        "session_id": session_id,
        "public_id": public_id,
        "anon_name": anon_name,
        "anon_avatar": anon_avatar,
        "ip": ip,
        "hostname": hostname,
        "mac": mac,
        "user_agent": user_agent,
        "device_summary": device_summary,
        "real_name": real_name,
        "notes": notes,
        "is_banned": is_banned,
        "content": content,
        "image_path": image_path,
        "image_thumb": image_thumb,
        "image_name": image_name,
        "image_size": image_size,
        "image_dims": image_dims,
        "reply_to": reply_to,
        "reply_name": reply_name,
        "reply_snippet": reply_snippet,
        "room_id": room_id,
        "client_msg_id": client_msg_id,
        "reactions": {},
        "created_at": created_at,
        "is_deleted": 0
    }

def delete_message(post_num: int):
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("UPDATE messages SET is_deleted = 1 WHERE post_num = ?", (post_num,))
    conn.commit()
    conn.close()

def hide_message(post_num: int):
    delete_message(post_num)

def delete_all_from_ip(ip: str) -> tuple[int, List[int]]:
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT post_num FROM messages WHERE ip = ? AND is_deleted = 0", (ip,))
    post_nums = [r["post_num"] for r in cur.fetchall()]
    cur.execute("UPDATE messages SET is_deleted = 1 WHERE ip = ?", (ip,))
    count = cur.rowcount
    conn.commit()
    conn.close()
    return count, post_nums

def set_pinned(post_num: int, pinned: bool):
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("UPDATE messages SET is_pinned = ? WHERE post_num = ?", (1 if pinned else 0, post_num))
    conn.commit()
    conn.close()

def clear_messages():
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("DELETE FROM messages")
    cur.execute("DELETE FROM reactions")
    conn.commit()
    conn.close()

def toggle_reaction(post_num: int, emoji: str, session_id: str) -> Dict[str, int]:
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT id FROM reactions WHERE post_num = ? AND emoji = ? AND session_id = ?", (post_num, emoji, session_id))
    row = cur.fetchone()
    if row:
        cur.execute("DELETE FROM reactions WHERE id = ?", (row["id"],))
    else:
        now = datetime.utcnow().isoformat()
        cur.execute("INSERT INTO reactions (post_num, emoji, session_id, created_at) VALUES (?, ?, ?, ?)", (post_num, emoji, session_id, now))
    conn.commit()

    # Get updated counts
    cur.execute("SELECT emoji, COUNT(*) as count FROM reactions WHERE post_num = ? GROUP BY emoji", (post_num,))
    counts = {r["emoji"]: r["count"] for r in cur.fetchall()}
    conn.close()
    return counts

def get_reactions_for_posts(post_nums: List[int]) -> Dict[int, Dict[str, int]]:
    if not post_nums:
        return {}
    conn = get_db_connection()
    cur = conn.cursor()
    placeholders = ",".join("?" for _ in post_nums)
    cur.execute(f"""
        SELECT post_num, emoji, COUNT(*) as count 
        FROM reactions 
        WHERE post_num IN ({placeholders}) 
        GROUP BY post_num, emoji
    """, post_nums)
    rows = cur.fetchall()
    conn.close()

    result = {pn: {} for pn in post_nums}
    for r in rows:
        result[r["post_num"]][r["emoji"]] = r["count"]
    return result

def get_messages(room_id: str = "main", limit: int = 150, is_host: bool = False, before_post_num: Optional[int] = None) -> List[Dict[str, Any]]:
    conn = get_db_connection()
    cur = conn.cursor()
    params = [room_id, room_id]
    before_clause = ""
    if before_post_num is not None:
        before_clause = " AND m.post_num < ? "
        params.append(before_post_num)
    params.append(limit)

    query = f"""
        SELECT * FROM (
            SELECT 
                m.*,
                rm.anon_name AS reply_name,
                CASE 
                    WHEN rm.content IS NOT NULL AND LENGTH(TRIM(rm.content)) > 0 THEN SUBSTR(rm.content, 1, 100)
                    WHEN rm.image_name IS NOT NULL THEN 'Photo: ' || rm.image_name
                    WHEN rm.image_path IS NOT NULL THEN 'Photo'
                    ELSE ''
                END AS reply_snippet,
                i.real_name AS id_real_name,
                i.notes AS id_notes,
                i.is_banned AS id_is_banned
            FROM messages m
            LEFT JOIN messages rm ON m.reply_to = rm.post_num
            LEFT JOIN identities i ON m.ip = i.ip
            WHERE m.is_deleted = 0 AND (m.room_id = ? OR (? = 'main' AND (m.room_id IS NULL OR m.room_id = 'main' OR m.room_id = ''))){before_clause}
            ORDER BY m.post_num DESC
            LIMIT ?
        )
        ORDER BY is_pinned DESC, post_num ASC
    """
    cur.execute(query, tuple(params))
    rows = cur.fetchall()
    conn.close()

    post_nums = [r["post_num"] for r in rows]
    reactions_map = get_reactions_for_posts(post_nums)

    result = []
    for r in rows:
        d = dict(r)
        d["reactions"] = reactions_map.get(d["post_num"], {})
        d["reply_name"] = d.get("reply_name") or ""
        d["reply_snippet"] = d.get("reply_snippet") or ""
        if not is_host:
            # Strip private info from public view
            d.pop("session_id", None)
            d.pop("ip", None)
            d.pop("hostname", None)
            d.pop("mac", None)
            d.pop("user_agent", None)
            d.pop("device_summary", None)
            d.pop("id_real_name", None)
            d.pop("id_notes", None)
            d.pop("id_is_banned", None)
        else:
            d.pop("session_id", None)

            # Map joined fields
            d["real_name"] = d.get("id_real_name") or ""
            d["notes"] = d.get("id_notes") or ""
            d["is_banned"] = bool(d.get("id_is_banned") == 1)
            d.pop("id_real_name", None)
            d.pop("id_notes", None)
            d.pop("id_is_banned", None)
        result.append(d)
    return result

def create_private_room(room_id: str, name: str, created_by: str, members: List[Dict[str, str]]) -> Dict[str, Any]:
    now = datetime.utcnow().isoformat()
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("""
        INSERT OR REPLACE INTO rooms (room_id, name, created_by, created_at, is_active)
        VALUES (?, ?, ?, ?, 1)
    """, (room_id, name, created_by, now))

    for m in members:
        cur.execute("""
            INSERT OR REPLACE INTO room_members (room_id, public_id, anon_name, anon_avatar, joined_at)
            VALUES (?, ?, ?, ?, ?)
        """, (room_id, m["public_id"], m.get("anon_name", ""), m.get("anon_avatar", ""), now))

    conn.commit()
    conn.close()
    return {
        "room_id": room_id,
        "name": name,
        "created_by": created_by,
        "created_at": now,
        "members": members
    }

def is_room_admin(room_id: str, public_id: str) -> bool:
    """Returns True if the public_id is the owner or an admin of the room."""
    if not room_id or not public_id or room_id in PUBLIC_ROOMS:
        return False
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT created_by, owner_public_id FROM rooms WHERE room_id = ?", (room_id,))
    room = cur.fetchone()
    if not room:
        conn.close()
        return False
    owner_id = (room["owner_public_id"] or room["created_by"] or "").strip()
    if owner_id and owner_id == public_id.strip():
        conn.close()
        return True
    cur.execute("SELECT role FROM room_members WHERE room_id = ? AND public_id = ?", (room_id, public_id))
    member = cur.fetchone()
    conn.close()
    if member and member["role"] and member["role"].strip().upper() in ("ADMIN", "OWNER"):
        return True
    return False

def create_group_room(room_id: str, name: str, desc: str, owner_public_id: str, creator_anon_name: str, creator_anon_avatar: str, is_public: bool = True) -> Dict[str, Any]:
    now = datetime.utcnow().isoformat()
    is_public_int = 1 if is_public else 0
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("""
        INSERT OR REPLACE INTO rooms (room_id, name, desc, created_by, created_at, is_active, is_public, owner_public_id)
        VALUES (?, ?, ?, ?, ?, 1, ?, ?)
    """, (room_id, name, desc, owner_public_id, now, is_public_int, owner_public_id))
    cur.execute("""
        INSERT OR REPLACE INTO room_members (room_id, public_id, anon_name, anon_avatar, joined_at, role)
        VALUES (?, ?, ?, ?, ?, 'ADMIN')
    """, (room_id, owner_public_id, creator_anon_name, creator_anon_avatar, now))
    conn.commit()
    conn.close()
    return {
        "room_id": room_id,
        "name": name,
        "description": desc,
        "desc": desc,
        "created_by": owner_public_id,
        "owner_public_id": owner_public_id,
        "created_at": now,
        "is_public": is_public,
        "is_owner": True,
        "is_admin": True,
        "members": [{
            "room_id": room_id,
            "public_id": owner_public_id,
            "anon_name": creator_anon_name,
            "anon_avatar": creator_anon_avatar,
            "joined_at": now,
            "role": "ADMIN",
            "is_owner": 1,
            "is_admin": 1
        }]
    }

def update_room_name(room_id: str, new_name: str, public_id: str = "") -> bool:
    """Updates group name. Only allowed if user is group admin/owner."""
    if not room_id or room_id in PUBLIC_ROOMS or not new_name:
        return False
    if public_id and not is_room_admin(room_id, public_id):
        return False
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("UPDATE rooms SET name = ? WHERE room_id = ?", (new_name, room_id))
    conn.commit()
    conn.close()
    return True

def update_room_desc(room_id: str, new_desc: str, public_id: str = "") -> bool:
    """Updates group description/topic. Only allowed if user is group admin/owner."""
    if not room_id or room_id in PUBLIC_ROOMS:
        return False
    if public_id and not is_room_admin(room_id, public_id):
        return False
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("UPDATE rooms SET desc = ? WHERE room_id = ?", (new_desc, room_id))
    conn.commit()
    conn.close()
    return True

def update_room_visibility(room_id: str, is_public: bool, public_id: str = "") -> bool:
    """Updates group visibility (public vs private). Only allowed if user is group admin/owner."""
    if not room_id or room_id in PUBLIC_ROOMS:
        return False
    if public_id and not is_room_admin(room_id, public_id):
        return False
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("UPDATE rooms SET is_public = ? WHERE room_id = ?", (1 if is_public else 0, room_id))
    conn.commit()
    conn.close()
    return True

def get_user_room_ids(public_id: str) -> List[str]:
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT room_id FROM room_members WHERE public_id = ?", (public_id,))
    rows = cur.fetchall()
    conn.close()
    return [r["room_id"] for r in rows if r["room_id"] not in PUBLIC_ROOMS]

def delete_group_room(room_id: str, public_id: str) -> Tuple[bool, str]:
    """Deletes/disbands group. Only allowed if user is group admin/owner."""
    if not room_id or room_id in PUBLIC_ROOMS:
        return False, "Cannot delete public rooms"
    if not is_room_admin(room_id, public_id):
        return False, "Only group admin can delete or disband this group"
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT name FROM rooms WHERE room_id = ?", (room_id,))
    row = cur.fetchone()
    if not row:
        conn.close()
        return False, "Room not found"
    room_name = row["name"]
    cur.execute("DELETE FROM room_members WHERE room_id = ?", (room_id,))
    cur.execute("DELETE FROM messages WHERE room_id = ?", (room_id,))
    cur.execute("DELETE FROM rooms WHERE room_id = ?", (room_id,))
    conn.commit()
    conn.close()
    return True, room_name

def add_user_to_room(room_id: str, public_id: str, anon_name: str, anon_avatar: str, role: str = "MEMBER") -> bool:
    now = datetime.utcnow().isoformat()
    conn = get_db_connection()
    cur = conn.cursor()

    # Check if this user is the room creator/owner or existing admin
    cur.execute("SELECT owner_public_id, created_by FROM rooms WHERE room_id = ?", (room_id,))
    room_owner = cur.fetchone()
    owner_id = (room_owner["owner_public_id"] or room_owner["created_by"] or "").strip() if room_owner else ""

    cur.execute("SELECT role FROM room_members WHERE room_id = ? AND public_id = ?", (room_id, public_id))
    existing = cur.fetchone()

    if (owner_id and owner_id == public_id.strip()) or (existing and existing["role"] and existing["role"].strip().upper() in ("ADMIN", "OWNER")):
        role = "ADMIN"

    cur.execute("""
        INSERT OR REPLACE INTO room_members (room_id, public_id, anon_name, anon_avatar, joined_at, role)
        VALUES (?, ?, ?, ?, ?, ?)
    """, (room_id, public_id, anon_name, anon_avatar, now, role))

    # Update room name if room now has multiple members
    cur.execute("SELECT anon_name FROM room_members WHERE room_id = ?", (room_id,))
    m_rows = cur.fetchall()
    member_names = [r["anon_name"].split(" #")[0] for r in m_rows if r["anon_name"]]
    if len(member_names) > 2:
        new_name = f"Group: {', '.join(member_names[:3])}" + (f" (+{len(member_names)-3})" if len(member_names) > 3 else "")
        cur.execute("UPDATE rooms SET name = ? WHERE room_id = ?", (new_name, room_id))

    conn.commit()
    conn.close()
    return True

def remove_user_from_room(room_id: str, public_id: str) -> bool:
    """
    Removes user from room. Returns True if room had 0 members remaining and was permanently deleted.
    """
    if room_id in PUBLIC_ROOMS:
        return False
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("DELETE FROM room_members WHERE room_id = ? AND public_id = ?", (room_id, public_id))

    # Check remaining members
    cur.execute("SELECT anon_name FROM room_members WHERE room_id = ?", (room_id,))
    m_rows = cur.fetchall()
    was_deleted = False
    if not m_rows:
        # If no members remain, permanently delete the group
        cur.execute("DELETE FROM room_members WHERE room_id = ?", (room_id,))
        cur.execute("DELETE FROM messages WHERE room_id = ?", (room_id,))
        cur.execute("DELETE FROM rooms WHERE room_id = ?", (room_id,))
        was_deleted = True
    else:
        member_names = [r["anon_name"].split(" #")[0] for r in m_rows if r["anon_name"]]
        if len(member_names) > 2:
            new_name = f"Group: {', '.join(member_names[:3])}" + (f" (+{len(member_names)-3})" if len(member_names) > 3 else "")
        elif len(member_names) == 2:
            new_name = f"{member_names[0]} & {member_names[1]}"
        elif len(member_names) == 1:
            new_name = f"Chat: {member_names[0]}"
        else:
            new_name = "Empty Chat"
        cur.execute("UPDATE rooms SET name = ? WHERE room_id = ?", (new_name, room_id))

    conn.commit()
    conn.close()
    return was_deleted

def remove_user_from_all_rooms(public_id: str) -> List[str]:
    """Removes user from all private groups. Permanently deletes any groups that have no members left."""
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT room_id FROM room_members WHERE public_id = ?", (public_id,))
    rows = cur.fetchall()
    room_ids = [r["room_id"] for r in rows if r["room_id"] not in PUBLIC_ROOMS]
    deleted_rooms = []
    
    for r_id in room_ids:
        cur.execute("DELETE FROM room_members WHERE room_id = ? AND public_id = ?", (r_id, public_id))
        cur.execute("SELECT COUNT(*) as cnt FROM room_members WHERE room_id = ?", (r_id,))
        cnt_row = cur.fetchone()
        if not cnt_row or cnt_row["cnt"] == 0:
            # Delete empty room
            cur.execute("DELETE FROM room_members WHERE room_id = ?", (r_id,))
            cur.execute("DELETE FROM messages WHERE room_id = ?", (r_id,))
            cur.execute("DELETE FROM rooms WHERE room_id = ?", (r_id,))
            deleted_rooms.append(r_id)
        else:
            cur.execute("SELECT anon_name FROM room_members WHERE room_id = ?", (r_id,))
            m_rows = cur.fetchall()
            member_names = [r["anon_name"].split(" #")[0] for r in m_rows if r["anon_name"]]
            if len(member_names) > 2:
                new_name = f"Group: {', '.join(member_names[:3])}" + (f" (+{len(member_names)-3})" if len(member_names) > 3 else "")
            elif len(member_names) == 2:
                new_name = f"{member_names[0]} & {member_names[1]}"
            elif len(member_names) == 1:
                new_name = f"Chat: {member_names[0]}"
            else:
                new_name = "Empty Chat"
            cur.execute("UPDATE rooms SET name = ? WHERE room_id = ?", (new_name, r_id))
            
    conn.commit()
    conn.close()
    return deleted_rooms

def get_room_members(room_id: str) -> List[Dict[str, Any]]:
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT owner_public_id, created_by FROM rooms WHERE room_id = ?", (room_id,))
    room_row = cur.fetchone()
    owner_id = (room_row["owner_public_id"] or room_row["created_by"] or "").strip() if room_row else ""

    cur.execute("SELECT room_id, public_id, anon_name, anon_avatar, joined_at, role FROM room_members WHERE room_id = ?", (room_id,))
    rows = cur.fetchall()
    conn.close()

    result = []
    for r in rows:
        d = dict(r)
        role = (d.get("role") or "MEMBER").strip()
        is_owner = 1 if (owner_id and d.get("public_id") == owner_id) else 0
        if is_owner:
            role = "ADMIN"
        is_admin = 1 if (is_owner == 1 or role.upper() in ("ADMIN", "OWNER")) else 0
        d["role"] = role
        d["is_owner"] = is_owner
        d["is_admin"] = is_admin
        result.append(d)
    return result

PUBLIC_ROOMS = {
    "main": "Public Chat",
    "cse": "CSE Stream",
    "ece": "Electronics Stream",
    "mech": "Mechanical Stream",
    "civil": "Civil Stream",
    "faculty": "Faculty",
    "hostel": "Hostel"
}

def is_user_in_room(room_id: str, public_id: str, is_host: bool = False) -> bool:
    if not room_id or room_id == "main" or room_id in PUBLIC_ROOMS or is_host:
        return True
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT 1 FROM room_members WHERE room_id = ? AND public_id = ?", (room_id, public_id))
    row = cur.fetchone()
    if not row:
        cur.execute("SELECT 1 FROM rooms WHERE room_id = ? AND (owner_public_id = ? OR created_by = ?)", (room_id, public_id, public_id))
        row = cur.fetchone()
    conn.close()
    return bool(row)

def get_room_info(room_id: str) -> Optional[Dict[str, Any]]:
    """Returns basic room info dict (room_id, name, is_public, is_active) or None if not found."""
    if not room_id:
        return None
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT room_id, name, is_public, is_active, created_by, owner_public_id FROM rooms WHERE room_id = ?", (room_id,))
    row = cur.fetchone()
    conn.close()
    return dict(row) if row else None

def get_user_rooms(public_id: str, is_host: bool = False) -> List[Dict[str, Any]]:
    conn = get_db_connection()
    cur = conn.cursor()
    placeholders = ",".join("?" for _ in PUBLIC_ROOMS.keys())
    if is_host:
        cur.execute(f"""
            SELECT r.* FROM rooms r
            WHERE r.is_active = 1
              AND r.room_id NOT IN ({placeholders})
              AND (r.created_by != 'system' OR r.created_by IS NULL)
            ORDER BY r.created_at DESC
        """, tuple(PUBLIC_ROOMS.keys()))
    else:
        cur.execute(f"""
            SELECT r.* FROM rooms r
            JOIN room_members rm ON r.room_id = rm.room_id
            WHERE rm.public_id = ? AND r.is_active = 1
              AND r.room_id NOT IN ({placeholders})
              AND (r.created_by != 'system' OR r.created_by IS NULL)
            ORDER BY r.created_at DESC
        """, (public_id, *PUBLIC_ROOMS.keys()))
    rows = cur.fetchall()
    conn.close()

    rooms = []
    for r in rows:
        d = dict(r)
        d["is_public"] = bool(d.get("is_public", 1))
        d["members"] = get_room_members(d["room_id"])
        owner_id = (d.get("owner_public_id") or d.get("created_by") or "").strip()
        is_owner = bool(owner_id and owner_id == public_id.strip())
        is_admin = is_owner or any(m.get("public_id") == public_id and (m.get("is_admin") == 1 or m.get("role") == "ADMIN") for m in d["members"])
        d["is_owner"] = is_owner
        d["is_admin"] = is_admin
        rooms.append(d)
    return rooms

def get_all_public_rooms() -> List[Dict[str, Any]]:
    """Returns only predefined public campus channels (PUBLIC_ROOMS) for the Chats tab. Groups created in Online are excluded."""
    conn = get_db_connection()
    cur = conn.cursor()
    placeholders = ",".join("?" for _ in PUBLIC_ROOMS.keys())
    cur.execute(f"""
        SELECT r.room_id AS id, r.name, r.desc, r.created_at,
               COUNT(rm.public_id) AS member_count
        FROM rooms r
        LEFT JOIN room_members rm ON r.room_id = rm.room_id
        WHERE r.is_active = 1 AND (r.room_id IN ({placeholders}) OR r.created_by = 'system')
        GROUP BY r.room_id
        ORDER BY r.created_at ASC
    """, tuple(PUBLIC_ROOMS.keys()))
    rows = cur.fetchall()
    conn.close()

    order_map = {k: idx for idx, k in enumerate(PUBLIC_ROOMS.keys())}
    result = [{"id": r["id"], "name": r["name"], "desc": r["desc"] if "desc" in r.keys() else "", "member_count": r["member_count"]} for r in rows]
    result.sort(key=lambda x: order_map.get(x["id"], 999))
    return result

def get_discoverable_public_groups() -> List[Dict[str, Any]]:
    """Returns only user-created discoverable public groups for the Online -> Groups tab. Predefined channels are excluded."""
    conn = get_db_connection()
    cur = conn.cursor()
    placeholders = ",".join("?" for _ in PUBLIC_ROOMS.keys())
    cur.execute(f"""
        SELECT r.room_id AS id, r.name, r.desc, r.created_at, r.created_by, r.owner_public_id,
               COUNT(rm.public_id) AS member_count
        FROM rooms r
        LEFT JOIN room_members rm ON r.room_id = rm.room_id
        WHERE r.is_active = 1 AND r.is_public = 1
          AND r.room_id NOT IN ({placeholders})
          AND (r.created_by != 'system' OR r.created_by IS NULL)
        GROUP BY r.room_id
        ORDER BY r.created_at DESC
    """, tuple(PUBLIC_ROOMS.keys()))
    rows = cur.fetchall()
    conn.close()
    return [
        {
            "id": r["id"],
            "room_id": r["id"],
            "name": r["name"],
            "desc": r["desc"] if "desc" in r.keys() else "",
            "description": r["desc"] if "desc" in r.keys() else "",
            "created_by": r["created_by"] if "created_by" in r.keys() else None,
            "owner_public_id": r["owner_public_id"] if "owner_public_id" in r.keys() else None,
            "is_public": True,
            "member_count": r["member_count"]
        }
        for r in rows
    ]

def delete_room(room_id: str) -> bool:
    if not room_id or room_id in PUBLIC_ROOMS:
        return False
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("DELETE FROM room_members WHERE room_id = ?", (room_id,))
    cur.execute("DELETE FROM messages WHERE room_id = ?", (room_id,))
    cur.execute("DELETE FROM rooms WHERE room_id = ?", (room_id,))
    conn.commit()
    conn.close()
    return True

def get_all_rooms_admin() -> List[Dict[str, Any]]:
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT * FROM rooms WHERE is_active = 1 ORDER BY created_at DESC")
    rows = cur.fetchall()
    conn.close()

    rooms = []
    for r in rows:
        d = dict(r)
        d["members"] = get_room_members(d["room_id"])
        conn2 = get_db_connection()
        c2 = conn2.cursor()
        c2.execute("SELECT COUNT(*) as count FROM messages WHERE room_id = ? AND is_deleted = 0", (d["room_id"],))
        cnt = c2.fetchone()
        conn2.close()
        d["message_count"] = cnt["count"] if cnt else 0
        rooms.append(d)
    return rooms

def get_all_identities() -> List[Dict[str, Any]]:
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("""
        SELECT 
            i.*,
            COUNT(m.id) as message_count
        FROM identities i
        LEFT JOIN messages m ON i.ip = m.ip AND m.is_deleted = 0
        GROUP BY i.ip
        ORDER BY i.last_seen DESC
    """)
    rows = cur.fetchall()
    conn.close()
    return [dict(r) for r in rows]

# --- Anonymous Feedback System ---
def insert_feedback(public_id: str, anon_name: str, ip: str, category: str, message: str, rating: int = 5) -> int:
    now = datetime.utcnow().isoformat()
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("""
        INSERT INTO feedbacks (public_id, anon_name, ip, category, message, rating, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
    """, (public_id, anon_name, ip, category, message, rating, now))
    fid = cur.lastrowid
    conn.commit()
    conn.close()
    return fid

def get_feedbacks(limit: int = 100) -> List[Dict[str, Any]]:
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT * FROM feedbacks ORDER BY id DESC LIMIT ?", (limit,))
    rows = cur.fetchall()
    conn.close()
    return [dict(r) for r in rows]

def delete_feedback(feedback_id: int) -> bool:
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("DELETE FROM feedbacks WHERE id = ?", (feedback_id,))
    deleted = cur.rowcount > 0
    conn.commit()
    conn.close()
    return deleted

# --- AI Admin Lookups & Moderation Actions ---
def get_post_info(post_num: int) -> Optional[Dict[str, Any]]:
    conn = get_db_connection()
    cur = conn.cursor()
    try:
        p_int = int(post_num)
    except (ValueError, TypeError):
        conn.close()
        return None
    cur.execute("""
        SELECT id, post_num, public_id, anon_name, ip, content, room_id, created_at, is_deleted 
        FROM messages WHERE post_num = ?
    """, (p_int,))
    row = cur.fetchone()
    if row:
        conn.close()
        return dict(row)
    cur.execute("SELECT post_num, anon_name, ip FROM ai_moderation_logs WHERE post_num = ? ORDER BY id DESC LIMIT 1", (p_int,))
    log_row = cur.fetchone()
    conn.close()
    if log_row:
        return {"post_num": p_int, "anon_name": log_row["anon_name"], "ip": log_row["ip"]}
    return None

def get_ip_by_public_id(public_id: str) -> Optional[str]:
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT ip FROM messages WHERE public_id = ? ORDER BY id DESC LIMIT 1", (public_id,))
    row = cur.fetchone()
    conn.close()
    return row["ip"] if row else None

def get_ip_by_author_name(anon_name: str) -> Optional[str]:
    if not anon_name:
        return None
    clean_name = anon_name.strip()
    conn = get_db_connection()
    cur = conn.cursor()
    # 1. Exact match in messages
    cur.execute("SELECT ip FROM messages WHERE anon_name = ? ORDER BY id DESC LIMIT 1", (clean_name,))
    row = cur.fetchone()
    if not row:
        # 2. Prefix match e.g. "Gray Wolf" matches "Gray Wolf #0b13"
        cur.execute("SELECT ip FROM messages WHERE anon_name LIKE ? ORDER BY id DESC LIMIT 1", (f"{clean_name}%",))
        row = cur.fetchone()
    if not row:
        # 3. Search identities table notes or real_name
        cur.execute("SELECT ip FROM identities WHERE real_name LIKE ? OR notes LIKE ? ORDER BY last_seen DESC LIMIT 1", (f"%{clean_name}%", f"%{clean_name}%"))
        row = cur.fetchone()
    conn.close()
    return row["ip"] if row else None

def clear_room_messages(room_id: str) -> int:
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("DELETE FROM messages WHERE room_id = ?", (room_id,))
    cnt = cur.rowcount
    conn.commit()
    conn.close()
    return cnt

def delete_multiple_messages(post_nums: List[int]) -> int:
    if not post_nums:
        return 0
    clean_nums = []
    for p in post_nums:
        try:
            clean_nums.append(int(p))
        except (ValueError, TypeError):
            pass
    if not clean_nums:
        return 0
    conn = get_db_connection()
    cur = conn.cursor()
    placeholders = ",".join("?" for _ in clean_nums)
    cur.execute(f"UPDATE messages SET is_deleted = 1 WHERE post_num IN ({placeholders})", clean_nums)
    cnt = cur.rowcount
    conn.commit()
    conn.close()
    return cnt

def get_setting(key: str, default: Optional[str] = None) -> Optional[str]:
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT value FROM server_settings WHERE key = ?", (key,))
    row = cur.fetchone()
    conn.close()
    return row["value"] if row else default

def set_setting(key: str, value: str):
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("INSERT OR REPLACE INTO server_settings (key, value) VALUES (?, ?)", (key, str(value)))
    conn.commit()
    conn.close()

def log_ai_moderation(post_num: Optional[int], anon_name: str, ip: str, action: str, reason: str, content: str = "") -> int:
    now = datetime.utcnow().isoformat()
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("""
        INSERT INTO ai_moderation_logs (post_num, anon_name, ip, action, reason, content, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
    """, (post_num, anon_name or "Unknown", ip or "", action, reason, content or "", now))
    lid = cur.lastrowid
    conn.commit()
    conn.close()
    return lid

def get_ai_moderation_logs(limit: int = 100) -> List[Dict[str, Any]]:
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("""
        SELECT id, post_num, anon_name, ip, action, reason, content, created_at
        FROM ai_moderation_logs
        ORDER BY id DESC LIMIT ?
    """, (limit,))
    rows = [dict(r) for r in cur.fetchall()]
    conn.close()
    return rows

def delete_ai_moderation_log(log_id: int) -> bool:
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("DELETE FROM ai_moderation_logs WHERE id = ?", (log_id,))
    deleted = cur.rowcount > 0
    conn.commit()
    conn.close()
    return deleted

def clear_ai_moderation_logs() -> int:
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("DELETE FROM ai_moderation_logs")
    deleted = cur.rowcount
    conn.commit()
    conn.close()
    return deleted


