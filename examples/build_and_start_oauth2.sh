#!/bin/bash
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Spins up the OAuth2 demo: WildFly (Hello API, OIDC-protected) + Keycloak.
#
# Both containers run in one Podman pod so they share the network namespace.
# That is what makes the token issuer URL (http://localhost:8081/realms/demo)
# identical from the browser's and from WildFly's point of view — otherwise
# "Authorize" succeeds but "Try it out" returns 401 (issuer mismatch).

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

POD_NAME=openapi-oauth2-demo
WILDFLY_CTR=oauth2-wildfly
KEYCLOAK_CTR=oauth2-keycloak
WILDFLY_IMG=openapi-oauth2-wildfly
KEYCLOAK_IMG=quay.io/keycloak/keycloak:26.0

echo "=== OpenAPI GUI Addon - OAuth2 Demo (Hello API + Keycloak) ==="
echo ""

echo "Building addon and Hello API..."
mvn clean install -q -pl addon -am
mvn clean install -q -pl examples/openapi-config -am
mvn clean package -q -pl examples/stage-runtime-example -am

echo "Starting Podman..."
podman machine start 2>/dev/null || true

echo "Tearing down previous demo (if any)..."
podman pod rm -f "$POD_NAME" 2>/dev/null || true

echo "Building WildFly image..."
cd "$SCRIPT_DIR"
podman build --no-cache -t "$WILDFLY_IMG" -f Dockerfile.oauth2 .

echo "Creating pod (ports 8080=WildFly, 8081=Keycloak)..."
podman pod create --name "$POD_NAME" -p 8080:8080 -p 8081:8081

echo "Starting Keycloak (realm 'demo' imported on first boot)..."
podman run -d --pod "$POD_NAME" --name "$KEYCLOAK_CTR" \
    -v "$SCRIPT_DIR/keycloak/realm-demo.json:/opt/keycloak/data/import/realm-demo.json:ro,Z" \
    -e KEYCLOAK_ADMIN=admin \
    -e KEYCLOAK_ADMIN_PASSWORD=admin \
    -e KC_HTTP_PORT=8081 \
    -e KC_HOSTNAME=localhost \
    -e KC_HOSTNAME_PORT=8081 \
    -e KC_HOSTNAME_STRICT=false \
    -e KC_HTTP_ENABLED=true \
    "$KEYCLOAK_IMG" start-dev --import-realm

echo "Waiting for Keycloak to be ready..."
for i in $(seq 1 60); do
    if curl -sf http://localhost:8081/realms/demo/.well-known/openid-configuration >/dev/null 2>&1; then
        echo "  Keycloak up."
        break
    fi
    sleep 2
done

echo "Starting WildFly..."
podman run -d --pod "$POD_NAME" --name "$WILDFLY_CTR" "$WILDFLY_IMG"

echo "Waiting for WildFly..."
sleep 10

cat <<EOF

=== Demo running ===

  Keycloak admin:    http://localhost:8081/  (admin / admin)
  Hello API (REST):  http://localhost:8080/hello-api/hello           [protected]
  OpenAPI doc:       http://localhost:8080/hello-api/openapi         [open]
  Swagger UI:        http://localhost:8080/hello-api/openapi-ui/     [open]

  Test user:         demo / demo

  Flow:
   1) Open the Swagger UI.
   2) Click "Authorize", accept the pre-filled openapi-ui client, click
      "Authorize" again — Keycloak login page opens. Log in as demo/demo.
   3) Expand GET /hello, click "Try it out" -> "Execute".
      Swagger UI sends "Authorization: Bearer <token>" and you should get
      "Hello demo from the Hello API!"

  Stop with:  podman pod rm -f $POD_NAME

EOF
