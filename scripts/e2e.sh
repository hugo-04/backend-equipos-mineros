#!/usr/bin/env bash
# Pruebas end-to-end contra un backend ya levantado (docker compose up).
# Valida: /health, login, CRUD, regla de mantenimiento y los errores 401/400/404/409/422.
#
# Uso:  BASE_URL=http://localhost:8080 ADMIN_USER=admin ADMIN_PASSWORD=... ./scripts/e2e.sh
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
: "${ADMIN_USER:?Falta ADMIN_USER}"
: "${ADMIN_PASSWORD:?Falta ADMIN_PASSWORD}"

CUERPO_FILE="$(mktemp)"
trap 'rm -f "$CUERPO_FILE"' EXIT

PASARON=0
STATUS=""
CUERPO=""
TOKEN=""

# request METODO RUTA [JSON] [usar_token=si|no]
request() {
  local metodo="$1" ruta="$2" body="${3:-}" auth="${4:-si}"
  local args=(-s -o "$CUERPO_FILE" -w '%{http_code}' -X "$metodo" "$BASE_URL$ruta")
  [ "$auth" = "si" ] && [ -n "$TOKEN" ] && args+=(-H "Authorization: Bearer $TOKEN")
  [ -n "$body" ] && args+=(-H 'Content-Type: application/json' -d "$body")
  STATUS="$(curl "${args[@]}")"
  CUERPO="$(cat "$CUERPO_FILE")"
}

# campo NOMBRE: saca el PRIMER valor simple con ese nombre en el JSON de la última respuesta
# (grep -o + head -1: así "id" es el del equipo y no el del "tipo" anidado que viene después)
campo() {
  printf '%s' "$CUERPO" | grep -o '"'"$1"'":\("[^"]*"\|[^,}]*\)' | head -1 | sed 's/^[^:]*://' | tr -d '"'
}

verificar() {
  local descripcion="$1" esperado="$2" real="$3"
  if [ "$esperado" != "$real" ]; then
    echo "  FALLO: $descripcion -> esperado '$esperado', obtenido '$real'"
    echo "  Respuesta: $CUERPO"
    exit 1
  fi
  PASARON=$((PASARON + 1))
  echo "  ok: $descripcion"
}

contiene() {
  local descripcion="$1" fragmento="$2"
  case "$CUERPO" in
    *"$fragmento"*) PASARON=$((PASARON + 1)); echo "  ok: $descripcion" ;;
    *) echo "  FALLO: $descripcion -> la respuesta no contiene '$fragmento'"; echo "  Respuesta: $CUERPO"; exit 1 ;;
  esac
}

echo "== E2E contra $BASE_URL =="

echo "[1] Health"
request GET /health "" no
verificar "GET /health responde 200" 200 "$STATUS"
verificar "status UP" UP "$(campo status)"

echo "[2] Seguridad"
request GET /equipos "" no
verificar "sin token a /equipos -> 401" 401 "$STATUS"
verificar "código NO_AUTENTICADO" NO_AUTENTICADO "$(campo code)"
request POST /auth/login "{\"username\":\"$ADMIN_USER\",\"password\":\"clave-incorrecta\"}" no
verificar "login con clave mala -> 401" 401 "$STATUS"
request POST /auth/login "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASSWORD\"}" no
verificar "login correcto -> 200" 200 "$STATUS"
TOKEN="$(campo accessToken)"
[ -n "$TOKEN" ] || { echo "  FALLO: el login no devolvió token"; exit 1; }

echo "[3] Catálogo"
request GET /tipos
verificar "GET /tipos -> 200" 200 "$STATUS"
contiene "incluye Excavadora" "Excavadora"

echo "[3b] Paginación"
request GET "/tipos?size=2"
verificar "GET /tipos?size=2 -> 200" 200 "$STATUS"
verificar "3 tipos repartidos en 2 páginas" 2 "$(campo totalPages)"
verificar "es la primera página" true "$(campo first)"
request GET "/tipos?page=1&size=2"
verificar "la segunda página trae el tipo restante y es la última" true "$(campo last)"
request GET "/equipos?size=101"
verificar "size mayor al máximo (100) -> 400" 400 "$STATUS"
request GET "/equipos?page=-1"
verificar "page negativa -> 400" 400 "$STATUS"

echo "[4] Validaciones (400)"
request POST /equipos '{"codigo":"a b","tipoId":0,"horometro":-1}'
verificar "body inválido -> 400" 400 "$STATUS"
contiene "detalle del campo codigo" '"field":"codigo"'
request POST /equipos '{"codigo":"CAM-X","tipoId":1,"horometro":5,"extra":1}'
verificar "propiedad desconocida -> 400" 400 "$STATUS"
request GET /equipos/0
verificar "id 0 -> 400" 400 "$STATUS"

echo "[5] CRUD y regla de mantenimiento"
CODIGO="E2E-$(date +%s)"
request POST /equipos "{\"codigo\":\"$CODIGO\",\"tipoId\":1,\"horometro\":249.9}"
verificar "crear equipo -> 201" 201 "$STATUS"
ID="$(campo id)"
verificar "249.9 h de 250 -> no requiere mantenimiento" false "$(campo requiereMantenimiento)"
request POST /equipos "{\"codigo\":\"$CODIGO\",\"tipoId\":1,\"horometro\":1}"
verificar "código duplicado -> 409" 409 "$STATUS"
request GET /equipos/999999999
verificar "equipo inexistente -> 404" 404 "$STATUS"
request PUT "/equipos/$ID" "{\"codigo\":\"$CODIGO\",\"tipoId\":1,\"horometro\":250.0}"
verificar "editar a 250 h -> 200" 200 "$STATUS"
verificar "250 h (justo el intervalo) -> requiere mantenimiento" true "$(campo requiereMantenimiento)"
request PUT "/equipos/$ID" "{\"codigo\":\"$CODIGO\",\"tipoId\":1,\"horometro\":100.0}"
verificar "bajar el horómetro -> 422" 422 "$STATUS"
request POST "/equipos/$ID/mantenimientos" '{"notas":"Cambio de aceite (e2e)"}'
verificar "registrar mantenimiento -> 201" 201 "$STATUS"
request GET "/equipos/$ID"
verificar "tras el mantenimiento el ciclo se reinicia" false "$(campo requiereMantenimiento)"
verificar "horometroUltimoMant = 250.0" 250.0 "$(campo horometroUltimoMant)"
request GET "/equipos/$ID/mantenimientos"
verificar "historial -> 200" 200 "$STATUS"
contiene "historial con la nota" "Cambio de aceite (e2e)"

echo "[6] Baja"
request DELETE "/equipos/$ID"
verificar "eliminar -> 204" 204 "$STATUS"
request GET "/equipos/$ID"
verificar "ya no existe -> 404" 404 "$STATUS"
request GET "/equipos/$ID/mantenimientos"
verificar "su historial tampoco -> 404" 404 "$STATUS"

echo "== E2E OK: $PASARON verificaciones =="
