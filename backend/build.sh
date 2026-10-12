#!/usr/bin/env bash
# Render build script for Wathb backend.
# Run from the repository root: bash backend/build.sh
set -euo pipefail

cd "$(dirname "$0")"   # ensure we are in backend/

pip install --upgrade pip
pip install -r requirements.txt

python manage.py collectstatic --noinput
python manage.py migrate
python manage.py ensure_admin
