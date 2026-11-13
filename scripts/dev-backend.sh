#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
"$project_dir/scripts/setup-dev-env.sh"

set -a
source "$project_dir/.env"
set +a

cd "$project_dir"
if [[ "${SPRING_DATASOURCE_URL:-}" == 'jdbc:postgresql://localhost:5432/academia_pruebas' ]]; then
  database_exists="$(docker compose exec -T postgres psql -U "$POSTGRES_USER" -d postgres -t -A \
    -c "SELECT 1 FROM pg_database WHERE datname = 'academia_pruebas'")"
  if [[ "$database_exists" != 1 ]]; then
    docker compose exec -T postgres createdb -U "$POSTGRES_USER" -O "$POSTGRES_USER" academia_pruebas
  fi
fi

exec mvn spring-boot:run -Dspring-boot.run.profiles=dev
