# Environment Audit — Yawmiyati

Completed: 2026-10-10

## Repository State

- Branch: `main`
- Remote: `https://github.com/IV7L/yawmiyati.git`
- Commits: 1 (initialization commit)
- Working tree: clean
- Existing files: `.gitignore` (minimal)

## System

- macOS: Darwin 24.6.0 (Sequoia)
- Architecture: x86_64
- RAM: 16 GB
- Free disk: ~110 GB

## Python

| Tool | Version | Path |
|------|---------|------|
| System Python | 3.14.3 | `/Library/Frameworks/Python.framework/Versions/3.14/bin/python3` |
| pyenv | installed | `~/.pyenv/shims/python3` |
| Python 3.11.9 | via pyenv | available |
| Python 3.12.13 | via pyenv | available |
| Python 3.13.3 | via pyenv | available |

**Decision:** Use Python 3.12.13 (pyenv) for the Django backend. 3.12 is LTS-track, fully supported by Django 5.x, and stable. 3.14 is too new for reliable dependency support.

The project `.python-version` file in `backend/` will pin 3.12.13.

## Java / JDK

| Component | Version | Path |
|-----------|---------|------|
| OpenJDK | 17.0.18 | `/usr/local/Cellar/openjdk@17/17.0.18` |
| javac | 17.0.18 | Homebrew |

OpenJDK 17 is the Android-recommended LTS JDK for Android Studio and Gradle. Compatible.

## Android

| Component | Status |
|-----------|--------|
| Android Studio | 2026.2.1 — installed at `/Applications/Android Studio.app` |
| Android SDK | **NOT configured** — no SDK downloaded to standard location |
| adb | 1.0.41 / 36.0.2 — installed via `android-platform-tools` Homebrew cask |
| AVDs | None configured |
| ANDROID_HOME / ANDROID_SDK_ROOT | Not set |

Android Studio is installed but the SDK has never been downloaded. Before Android development can start:

1. Open Android Studio
2. Go to **SDK Manager** (Tools → SDK Manager)
3. Install: **Android SDK Platform 35** (API 35, Android 15) and at minimum API 26 (Android 8)
4. Install: **Android SDK Build-Tools** (latest)
5. Install: **Android Emulator**
6. Install: **Android SDK Platform-Tools**
7. Create an AVD (Pixel 6 or similar, API 35)
8. Note the SDK path shown in SDK Manager (default: `~/Library/Android/sdk`)
9. Set `ANDROID_HOME` in shell profile

Until the SDK is configured, the Android application cannot be built or tested. Django backend development can proceed immediately.

## PostgreSQL

| Component | Status |
|-----------|--------|
| PostgreSQL 14.22 | Running (via Homebrew, `postgresql@14`) |
| Port | 5432 |
| Local user | `macbook` (superuser) |
| psycopg2 | 2.9.12 — installed system-wide |
| PostgreSQL 15, 17 | Installed but erroring on startup — not needed |

PostgreSQL 14 is running and accessible. No Docker required.

## Other Tools

| Tool | Status |
|------|--------|
| Git | 2.52.0 |
| Homebrew | 6.0.19 |
| Docker Desktop | Installed (cask 4.49.0) but daemon not running |
| VirtualBox | Installed — not needed for this project |

## Python Packages (system Python)

- `psycopg2` 2.9.12 — available system-wide
- Django — **not installed** (will install in venv)
- djangorestframework — **not installed**

## Architecture Decision

Local development will use:
- Python 3.12 virtual environment in `backend/.venv`
- PostgreSQL 14 (local, already running)
- No Docker (not needed)
- Android Studio SDK Manager for Android SDK setup (manual step required before Milestone 6)

The Android emulator connects to the Django dev server on Mac via `http://10.0.2.2:8000/`.

## Missing Prerequisites

| Prerequisite | Action Required |
|---|---|
| Android SDK | Open Android Studio → SDK Manager → install Platform 35 + Build Tools + Emulator |
| ANDROID_HOME env var | Set after SDK install: `export ANDROID_HOME=~/Library/Android/sdk` |
| Django venv | Created as part of Milestone 2 setup |

## Compatibility Notes

- Java 17 is correct for Gradle 8+ and Android Studio 2024+
- PostgreSQL 14 supports all required Django features
- Python 3.12 is fully compatible with Django 5.1+
- macOS x86_64 (Intel) — all tools support this architecture

## Setup Commands (when needed)

```bash
# Create and activate Python 3.12 venv for backend
cd backend
pyenv local 3.12.13
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# Create PostgreSQL database
createdb yawmiyati_dev
createdb yawmiyati_test

# Android SDK (after SDK Manager install)
echo 'export ANDROID_HOME=~/Library/Android/sdk' >> ~/.zshrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator' >> ~/.zshrc
source ~/.zshrc
```
