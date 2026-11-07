#!/usr/bin/env bash
# =====================================================================================
# deploy.sh - Despliegue con verificación y rollback automático.
#
# ÚNICA fuente de la lógica de despliegue: GitHub Actions solo prepara el entorno y
# ejecuta este mismo script (también disponible como "make deploy").
#
# Pasos:
#   1. Verifica herramientas y variables de entorno
#   2. mvn verify (pruebas + cobertura >= 90%)
#   3. Construye la imagen y la etiqueta con el SHA corto del commit
#   4. Guarda la versión que está desplegada ahora (para poder volver a ella)
#   5. Despliega la versión nueva (las migraciones Flyway corren al arrancar la app)
#   6. Verifica /health con reintentos y confirma que corre la versión nueva
#   7. Si el paso 5 o 6 falla: ROLLBACK automático al tag guardado en el paso 4
#
# Códigos de salida:
#   0  despliegue correcto
#   1  error antes de tocar producción (variables, herramientas, pruebas, build)
#   2  el despliegue falló y el ROLLBACK se ejecutó bien (producción quedó en la versión previa)
#   3  el despliegue falló y NO había versión previa a la que volver (primer despliegue)
#   4  el despliegue falló y el ROLLBACK TAMBIÉN falló (intervención manual urgente)
#
# Variables obligatorias:
#   RAILWAY_TOKEN     token de proyecto de Railway
#   RAILWAY_SERVICE   nombre o id del servicio en Railway
#   APP_URL           URL pública del servicio (ej. https://equipos.up.railway.app)
#   REGISTRY_IMAGE    imagen sin tag (ej. ghcr.io/mi-org/equipos-mineros)
# Variables opcionales:
#   RAILWAY_ENVIRONMENT  entorno de Railway (si no se indica, usa el del token)
#   HEALTH_RETRIES       intentos de /health (por defecto 10)
#   HEALTH_INTERVAL      segundos entre intentos (por defecto 5)
#   HEALTH_TIMEOUT       segundos máximos por petición (por defecto 5)
#   DEPLOY_STATE_DIR     carpeta donde se guardan los tags (por defecto .deploy)
#   SIMULATE_FAILURE     "health" fuerza que la verificación de la versión nueva falle,
#                        para DEMOSTRAR el rollback. Desactivado por defecto.
# =====================================================================================
set -Eeuo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

# ---------- Código de salida ----------
readonly EXIT_OK=0
readonly EXIT_PRECONDICION=1
readonly EXIT_ROLLBACK_OK=2
readonly EXIT_SIN_PREVIA=3
readonly EXIT_ROLLBACK_FALLO=4

# ---------- Configuración ----------
HEALTH_RETRIES="${HEALTH_RETRIES:-10}"
HEALTH_INTERVAL="${HEALTH_INTERVAL:-5}"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-5}"
DEPLOY_STATE_DIR="${DEPLOY_STATE_DIR:-.deploy}"
SIMULATE_FAILURE="${SIMULATE_FAILURE:-}"

PASO_ACTUAL="inicio"
TAG=""
PREVIOUS_TAG=""
HTTP_CODE=""
HTTP_BODY=""

# ---------- Utilidades de log ----------
ahora() { date -u +'%Y-%m-%dT%H:%M:%SZ'; }
log() { printf '[%s] %s\n' "$(ahora)" "$*"; }
paso() {
  PASO_ACTUAL="$1"
  printf '\n[%s] ==> PASO %s\n' "$(ahora)" "$1"
}
error() { printf '[%s] ERROR: %s\n' "$(ahora)" "$*" >&2; }

# Si algo falla sin manejo explícito, dice QUÉ paso falló y en qué línea
al_fallar() {
  local codigo=$? linea=$1
  error "falló el paso \"$PASO_ACTUAL\" (línea $linea, código $codigo). No se tocó producción antes del paso 5."
  exit "$EXIT_PRECONDICION"
}
trap 'al_fallar $LINENO' ERR

# ---------- Paso 1: herramientas y variables ----------
verificar_requisitos() {
  local faltan=0 herramienta variable
  for herramienta in git docker mvn railway curl; do
    if ! command -v "$herramienta" >/dev/null 2>&1; then
      error "no se encontró la herramienta '$herramienta' en el PATH. Instálala e intenta de nuevo."
      faltan=1
    fi
  done
  for variable in RAILWAY_TOKEN RAILWAY_SERVICE APP_URL REGISTRY_IMAGE; do
    if [ -z "${!variable:-}" ]; then
      error "falta la variable de entorno $variable (ver el encabezado de deploy.sh y el README)."
      faltan=1
    fi
  done
  case "$SIMULATE_FAILURE" in
    ""|health) ;;
    *) error "SIMULATE_FAILURE='$SIMULATE_FAILURE' no es válido. Valores permitidos: vacío o 'health'."; faltan=1 ;;
  esac
  [ "$faltan" -eq 0 ] || exit "$EXIT_PRECONDICION"
  APP_URL="${APP_URL%/}"
  log "Herramientas y variables OK (servicio=$RAILWAY_SERVICE, imagen=$REGISTRY_IMAGE)"
}

# ---------- HTTP ----------
# http_get URL -> deja el código en HTTP_CODE y el cuerpo en HTTP_BODY ("000" si no hubo respuesta)
http_get() {
  local salida
  salida="$(mktemp)"
  HTTP_CODE="$(curl -sS --max-time "$HEALTH_TIMEOUT" -o "$salida" -w '%{http_code}' "$1" 2>/dev/null)" || HTTP_CODE="000"
  HTTP_BODY="$(cat "$salida" 2>/dev/null || true)"
  rm -f "$salida"
}

version_de() {
  printf '%s' "$1" | sed -n 's/.*"version" *: *"\([^"]*\)".*/\1/p' | head -1
}

# ---------- Paso 4: versión desplegada ----------
detectar_version_desplegada() {
  http_get "$APP_URL/health"
  local version
  version="$(version_de "$HTTP_BODY")"
  case "$version" in
    ""|dev|local|test) echo "" ;;   # no es un tag de imagen al que se pueda volver
    *) echo "$version" ;;
  esac
}

# ---------- Despliegue en el hosting (Railway) ----------
# Railway no permite elegir un tag de imagen desde su CLI, así que se sube una carpeta
# con un Dockerfile de una línea (FROM <imagen>:<tag>). Desplegar y hacer rollback son
# la MISMA operación con distinto tag: no hay dos caminos que mantener.
desplegar_tag() {
  local tag="$1" carpeta
  carpeta="$(mktemp -d)"
  printf 'FROM %s:%s\n' "$REGISTRY_IMAGE" "$tag" >"$carpeta/Dockerfile"
  log "Desplegando $REGISTRY_IMAGE:$tag en Railway (servicio $RAILWAY_SERVICE)..."
  local args=(up "$carpeta" --service "$RAILWAY_SERVICE" --ci)
  [ -n "${RAILWAY_ENVIRONMENT:-}" ] && args+=(--environment "$RAILWAY_ENVIRONMENT")
  local resultado=0
  railway "${args[@]}" || resultado=$?
  rm -rf "$carpeta"
  return "$resultado"
}

# ¿La versión que queremos desplegar ya está arriba y sana? (una sola consulta, sin reintentos)
ya_desplegada_y_sana() {
  http_get "$APP_URL/health"
  [ "$HTTP_CODE" = "200" ] && [ "$(version_de "$HTTP_BODY")" = "$1" ]     && printf '%s' "$HTTP_BODY" | grep -q '"status" *: *"UP"'
}

# ---------- Paso 6: verificación ----------
# esperar_salud TAG [simulada]
esperar_salud() {
  local esperado="$1" simulada="${2:-}" url intento version estado
  url="$APP_URL/health"
  if [ "$simulada" = "simulada" ]; then
    url="$APP_URL/__fallo_simulado__/health"
    log "SIMULATE_FAILURE=health: la verificación apunta a $url, que NO existe, para forzar el fallo."
  fi
  for ((intento = 1; intento <= HEALTH_RETRIES; intento++)); do
    http_get "$url"
    version="$(version_de "$HTTP_BODY")"
    estado="$(printf '%s' "$HTTP_BODY" | sed -n 's/.*"status" *: *"\([^"]*\)".*/\1/p' | head -1)"
    log "Health intento $intento/$HEALTH_RETRIES: http=$HTTP_CODE status=${estado:--} version=${version:--} (se espera $esperado)"
    if [ "$HTTP_CODE" = "200" ] && [ "$estado" = "UP" ]; then
      if [ "$version" = "$esperado" ]; then
        log "Health OK: la versión $esperado está arriba y la base de datos responde."
        return 0
      fi
      log "El servicio responde pero con la versión '${version:--}', todavía no la $esperado."
    fi
    [ "$intento" -lt "$HEALTH_RETRIES" ] && sleep "$HEALTH_INTERVAL"
  done
  error "la versión $esperado no quedó saludable tras $HEALTH_RETRIES intentos."
  return 1
}

# ---------- Paso 7: rollback ----------
rollback() {
  local motivo="$1"
  trap - ERR
  printf '\n[%s] ==> PASO 7/7 ROLLBACK (motivo: %s)\n' "$(ahora)" "$motivo"
  if [ -z "$PREVIOUS_TAG" ]; then
    error "NO HAY ROLLBACK POSIBLE: era el primer despliegue y no existe una versión previa a la que volver."
    error "El despliegue de $TAG falló. Producción puede estar sin servicio. Revisa los logs de Railway."
    exit "$EXIT_SIN_PREVIA"
  fi
  log "Volviendo a la versión previa $PREVIOUS_TAG..."
  if desplegar_tag "$PREVIOUS_TAG" && esperar_salud "$PREVIOUS_TAG"; then
    printf '\n[%s] ############################################################\n' "$(ahora)"
    printf '[%s] ROLLBACK ejecutado a %s\n' "$(ahora)" "$PREVIOUS_TAG"
    printf '[%s] La versión %s NO quedó en producción (este despliegue se reporta como FALLIDO).\n' "$(ahora)" "$TAG"
    printf '[%s] ############################################################\n' "$(ahora)"
    exit "$EXIT_ROLLBACK_OK"
  fi
  printf '\n[%s] !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n' "$(ahora)" >&2
  error "EL ROLLBACK A $PREVIOUS_TAG TAMBIÉN FALLÓ. PRODUCCIÓN PUEDE ESTAR CAÍDA."
  error "Acción manual: redesplegar una versión buena desde el panel de Railway."
  printf '[%s] !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n' "$(ahora)" >&2
  exit "$EXIT_ROLLBACK_FALLO"
}

# =====================================================================================
main() {
  log "Inicio del despliegue"
  [ "$SIMULATE_FAILURE" = "health" ] &&
    log "ATENCIÓN: SIMULATE_FAILURE=health está activo. El despliegue FALLARÁ A PROPÓSITO para probar el rollback."

  paso "1/7 Verificando herramientas y variables de entorno"
  verificar_requisitos

  paso "2/7 Pruebas y cobertura (mvn verify, mínimo 90%)"
  mvn -B verify
  log "Pruebas y cobertura OK"

  paso "3/7 Construyendo y publicando la imagen"
  TAG="$(git rev-parse --short=7 HEAD)"
  local imagen="$REGISTRY_IMAGE:$TAG"
  log "Tag de esta versión: $TAG (nunca solo 'latest')"
  docker build --build-arg "APP_VERSION=$TAG" -t "$imagen" .
  docker push "$imagen"
  log "Imagen publicada: $imagen"

  paso "4/7 Guardando la versión actualmente desplegada"
  mkdir -p "$DEPLOY_STATE_DIR"
  PREVIOUS_TAG="$(detectar_version_desplegada)"
  if [ -n "$PREVIOUS_TAG" ]; then
    printf '%s\n' "$PREVIOUS_TAG" >"$DEPLOY_STATE_DIR/previous_tag"
    log "Versión previa guardada para rollback: $PREVIOUS_TAG"
  else
    rm -f "$DEPLOY_STATE_DIR/previous_tag"
    log "No hay versión previa desplegada (primer despliegue): si algo falla NO habrá a qué volver."
  fi

  # Idempotencia: ejecutarlo de nuevo sin cambios no vuelve a desplegar nada
  if [ -z "$SIMULATE_FAILURE" ] && ya_desplegada_y_sana "$TAG"; then
    paso "5/7 Despliegue omitido: $TAG ya está en producción y sana (el script es idempotente)"
    printf '\n[%s] DESPLIEGUE OK: %s ya estaba desplegada y saludable. Nada que hacer.\n' "$(ahora)" "$TAG"
    exit "$EXIT_OK"
  fi

  paso "5/7 Desplegando $TAG (las migraciones Flyway se aplican al arrancar la app)"
  trap - ERR   # a partir de aquí cualquier fallo se resuelve con rollback, no con "exit 1"
  if ! desplegar_tag "$TAG"; then
    error "el comando de despliegue falló."
    rollback "falló el despliegue de $TAG"
  fi

  paso "6/7 Verificando /health y la versión desplegada"
  local modo=""
  [ "$SIMULATE_FAILURE" = "health" ] && modo="simulada"
  if ! esperar_salud "$TAG" "$modo"; then
    rollback "la versión $TAG no pasó la verificación de salud"
  fi

  printf '\n[%s] DESPLIEGUE OK: %s está en producción y saludable (previa: %s).\n' \
    "$(ahora)" "$TAG" "${PREVIOUS_TAG:-ninguna}"
  exit "$EXIT_OK"
}

main "$@"
