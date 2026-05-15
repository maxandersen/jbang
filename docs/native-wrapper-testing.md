# Native wrapper local testing

This branch adds support for native-aware bundle bootstrap and update flows, plus `JBANG_DOWNLOAD_BASEURL` so the wrapper and installer can be tested against a local fake release tree.

## Quick local smoke test

Requirements:
- `tar`
- optional: `python3 -m http.server`

### 1. Create a fake release tree

```bash
tmpdir=$(mktemp -d)
mkdir -p "$tmpdir/releases/latest/download/jbang/bin"

cp src/main/scripts/jbang "$tmpdir/jbang"
chmod +x "$tmpdir/jbang"

cat > "$tmpdir/releases/latest/download/jbang/bin/jbang" <<'EOF'
#!/usr/bin/env bash
echo LOCAL_OK
EOF
chmod +x "$tmpdir/releases/latest/download/jbang/bin/jbang"

: > "$tmpdir/releases/latest/download/jbang/bin/jbang.jar"
echo 9.9.9 > "$tmpdir/releases/latest/download/version.txt"

tar cf "$tmpdir/releases/latest/download/jbang.tar" -C "$tmpdir/releases/latest/download" jbang
```

### 2. Test generic bundle bootstrap from local file URL

```bash
mkdir -p "$tmpdir/home"
HOME="$tmpdir/home" \
JBANG_DIR="$tmpdir/home/.jbang" \
JBANG_DOWNLOAD_BASEURL="file://$tmpdir/releases" \
"$tmpdir/jbang" version
```

Expected output includes:
- `Downloading JBang latest...`
- `Installing JBang...`
- `LOCAL_OK`

## Native bundle fallback test

If no native bundle exists, native mode should try the native URL first, then fall back to the generic bundle.

```bash
mkdir -p "$tmpdir/home-native"
HOME="$tmpdir/home-native" \
JBANG_DIR="$tmpdir/home-native/.jbang" \
JBANG_USE_NATIVE=true \
JBANG_DOWNLOAD_BASEURL="file://$tmpdir/releases" \
JBANG_DOWNLOAD_VERSION=0.138.0 \
"$tmpdir/jbang" version
```

Expected output includes:
- attempted native bundle download
- fallback warning
- generic install
- jar fallback if no native binary is present in the bundle

## Optional HTTP test

You can also serve the fake release tree over HTTP:

```bash
cd "$tmpdir/releases"
python3 -m http.server 18080
```

Then in another shell:

```bash
HOME="$tmpdir/home-http" \
JBANG_DIR="$tmpdir/home-http/.jbang" \
JBANG_DOWNLOAD_BASEURL="http://localhost:18080" \
"$tmpdir/jbang" version
```

## Update flow idea

To test update promotion end-to-end:
- prepare an installed `~/.jbang/bin` with existing `jbang.jar` and `jbang.bin`
- run `jbang version --update` with `JBANG_USE_NATIVE=true`
- verify `.new` files appear
- run wrapper again
- verify `.new` files are promoted into place

This branch implements the producer/consumer pieces needed for that flow.
