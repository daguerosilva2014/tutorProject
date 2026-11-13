#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
env_file="$project_dir/.env"

if [[ -e "$env_file" ]]; then
  printf 'Ya existe %s; no se modificó.\n' "$env_file"
  exit 0
fi

if ! command -v openssl >/dev/null 2>&1; then
  printf 'Se necesita openssl para generar las credenciales de desarrollo.\n' >&2
  exit 1
fi

jwt_secret="$(openssl rand -hex 32)"
admin_password="$(openssl rand -hex 18)"
umask 077
sed \
  -e "s/^JWT_SECRET=.*/JWT_SECRET=$jwt_secret/" \
  -e "s/^INITIAL_ADMIN_PASSWORD=.*/INITIAL_ADMIN_PASSWORD=$admin_password/" \
  "$project_dir/.env.example" > "$env_file"

printf 'Se creó .env para desarrollo (ignorado por Git).\n'
printf 'Usuario inicial: admin\n'
printf 'Contraseña inicial: %s\n' "$admin_password"
printf 'La contraseña también quedó en .env para consultarla localmente.\n'
