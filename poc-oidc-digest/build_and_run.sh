#!/usr/bin/env bash
#
# Build the addon + the three PoC WARs and launch the WildFly + Keycloak stack.
#
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$HERE/.." && pwd)"

echo "==> [1/4] Build & install the OpenAPI GUI addon (with per-module auth feature)"
mvn -q -f "$REPO_ROOT/pom.xml" -pl addon -am install -DskipTests

echo "==> [2/4] Build the three PoC WARs"
mvn -q -f "$HERE/pom.xml" clean package

echo "==> [3/4] Stage WARs for the WildFly image"
mkdir -p "$HERE/wildfly/deployments"
cp "$HERE/module-a-oidc/target/module-a.war"   "$HERE/wildfly/deployments/"
cp "$HERE/module-b-digest/target/module-b.war" "$HERE/wildfly/deployments/"

echo "==> [4/4] Build images & start the stack (podman compose)"
cd "$HERE"
podman compose up --build -d

cat <<'EOF'

----------------------------------------------------------------------
Stack starting (give WildFly + Keycloak ~30-60s).

Each module ships its own shared Swagger UI (open either — the dropdown lists both):

  Module A UI : http://localhost:8090/module-a/openapi-ui/
  Module B UI : http://localhost:8090/module-b/openapi-ui/
  Keycloak    : http://localhost:8081   (admin / admin)

In the Swagger UI dropdown (top right), switch between the two modules:

  Module A (OIDC)   -> click "Authorize" -> log in as  alice / alice
  Module B (Digest) -> "Try it out" -> prompted for     bob / bob

Logs : podman compose -f compose.yml logs -f
Stop : podman compose -f compose.yml down
----------------------------------------------------------------------
EOF
