#!/usr/bin/env python3
"""
AfterClass Release Tool: Cryptographic Update Metadata Signer
============================================================
Signs APK update metadata using the official release EC P-256 private key.
Generates an authentic version.json containing:
  - apkSha256: SHA-256 checksum of the production release APK.
  - signature: Base64-encoded ECDSA-SHA256 signature of canonical metadata.

Security Invariants:
  - Private key is NEVER embedded in the app or published to the repository.
  - Verification relies exclusively on the embedded public key in UpdateSecurityVerifier.
"""

import argparse
import base64
import hashlib
import json
import os
import subprocess
import sys
from pathlib import Path


def calculate_sha256(filepath: Path) -> str:
    sha = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            sha.update(chunk)
    return sha.hexdigest().lower()


def build_canonical_payload(
    apk_sha256: str,
    apk_url: str,
    force_update: bool,
    latest_version_code: int,
    latest_version_name: str,
    min_version_code: int,
) -> bytes:
    lines = [
        f"apkSha256={apk_sha256.strip().lower()}",
        f"apkUrl={apk_url.strip()}",
        f"forceUpdate={str(force_update).lower()}",
        f"latestVersionCode={latest_version_code}",
        f"latestVersionName={latest_version_name.strip()}",
        f"minimumSupportedVersionCode={min_version_code}",
    ]
    return "\n".join(lines).encode("utf-8")


def sign_payload_with_openssl(payload: bytes, private_key_path: Path) -> str:
    if not private_key_path.exists():
        raise FileNotFoundError(f"Private key file not found: {private_key_path}")

    proc = subprocess.Popen(
        ["openssl", "dgst", "-sha256", "-sign", str(private_key_path)],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    stdout, stderr = proc.communicate(payload)
    if proc.returncode != 0:
        raise RuntimeError(f"OpenSSL signing failed: {stderr.decode('utf-8', errors='ignore')}")

    return base64.b64encode(stdout).decode("ascii")


def verify_with_openssl(payload: bytes, signature_b64: str, public_key_pem: str) -> bool:
    sig_bytes = base64.b64decode(signature_b64)
    import tempfile

    with tempfile.NamedTemporaryFile("w", delete=False) as pub_f, tempfile.NamedTemporaryFile("wb", delete=False) as sig_f:
        pub_f.write(public_key_pem)
        pub_f_path = pub_f.name
        sig_f.write(sig_bytes)
        sig_f_path = sig_f.name

    try:
        proc = subprocess.Popen(
            ["openssl", "dgst", "-sha256", "-verify", pub_f_path, "-signature", sig_f_path],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        _, _ = proc.communicate(payload)
        return proc.returncode == 0
    finally:
        try: os.unlink(pub_f_path)
        except OSError: pass
        try: os.unlink(sig_f_path)
        except OSError: pass


def main():
    parser = argparse.ArgumentParser(description="Sign AfterClass APK update metadata")
    parser.add_argument("--apk", type=Path, required=True, help="Path to production release APK")
    parser.add_argument(
        "--private-key",
        type=Path,
        default=os.environ.get("UPDATE_SIGNING_PRIVATE_KEY_PATH", "update_signing_private_key.pem"),
        help="Path to EC P-256 private key PEM",
    )
    parser.add_argument("--version-json", type=Path, default=Path("anonchat/updates/version.json"), help="Path to version.json to update")
    parser.add_argument("--apk-url", type=str, default="https://api.afterclass.bond/updates/AfterClass.apk", help="Public HTTPS URL for APK download")
    parser.add_argument("--output", type=Path, help="Optional output path for signed version.json")

    args = parser.parse_args()

    if not args.apk.exists():
        print(f"Error: APK not found at {args.apk}", file=sys.stderr)
        sys.exit(1)

    private_key_path = Path(args.private_key)
    if not private_key_path.exists():
        print(f"Error: Private key not found at {private_key_path}", file=sys.stderr)
        print("Note: In release environments, set UPDATE_SIGNING_PRIVATE_KEY_PATH or pass --private-key", file=sys.stderr)
        sys.exit(1)

    apk_hash = calculate_sha256(args.apk)
    print(f"[+] APK SHA-256: {apk_hash}")

    data = {}
    if args.version_json.exists():
        with open(args.version_json, "r", encoding="utf-8") as f:
            data = json.load(f)

    latest_code = int(data.get("latestVersionCode", 3))
    latest_name = str(data.get("latestVersionName", "1.0.2"))
    apk_url = str(args.apk_url or data.get("apkUrl") or "/download/app.apk")
    force_update = bool(data.get("forceUpdate", False))
    min_version = int(data.get("minimumSupportedVersionCode", 1))

    canonical_payload = build_canonical_payload(
        apk_sha256=apk_hash,
        apk_url=apk_url,
        force_update=force_update,
        latest_version_code=latest_code,
        latest_version_name=latest_name,
        min_version_code=min_version,
    )

    signature = sign_payload_with_openssl(canonical_payload, private_key_path)
    print(f"[+] Signature: {signature[:20]}...{signature[-20:]}")

    data["latestVersionCode"] = latest_code
    data["latestVersionName"] = latest_name
    data["apkUrl"] = apk_url
    data["downloadUrl"] = apk_url
    data["forceUpdate"] = force_update
    data["minimumSupportedVersionCode"] = min_version
    data["apkSha256"] = apk_hash
    data["signature"] = signature

    out_path = args.output or args.version_json
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")

    print(f"[✓] Successfully wrote signed update metadata to {out_path}")


if __name__ == "__main__":
    main()
