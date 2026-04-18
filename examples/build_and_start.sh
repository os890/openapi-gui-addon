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

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

echo "=== OpenAPI GUI Addon - Demo Launcher ==="
echo ""

# --- Hello API (runtime project-stage) ---
echo "Hello API (runtime project-stage):"
echo "  1) development (OpenAPI UI enabled)"
echo "  2) production  (OpenAPI UI disabled)"
read -rp "  Choose [1/2]: " hello_choice
hello_choice=${hello_choice:-2}

if [ "$hello_choice" = "1" ]; then
    HELLO_STAGE="development"
    echo "  -> development"
else
    HELLO_STAGE="production"
    echo "  -> production"
fi

echo ""

# --- Greeting API (buildtime project-stage) ---
echo "Greeting API (buildtime Maven profile):"
echo "  1) development (OpenAPI UI enabled)"
echo "  2) production  (OpenAPI UI disabled)"
read -rp "  Choose [1/2]: " greeting_choice
greeting_choice=${greeting_choice:-2}

if [ "$greeting_choice" = "1" ]; then
    GREETING_PROFILE="-Pdevelopment"
    echo "  -> development"
else
    GREETING_PROFILE=""
    echo "  -> production"
fi

echo ""

# --- Info API (always on) ---
echo "Info API: always enabled (no project-stage)"
echo ""

# --- Build ---
echo "Building addon..."
mvn clean install -q -pl addon -am

echo "Building examples..."
mvn clean install -q -N -pl examples/pom.xml 2>/dev/null || true
mvn clean install -q -pl examples/openapi-config
mvn clean package -q -pl examples/stage-runtime-example
mvn clean package -q -pl examples/stage-buildtime-example $GREETING_PROFILE
mvn clean package -q -pl examples/stage-none-example

echo "Build complete."
echo ""

# --- Podman ---
echo "Starting Podman..."
podman machine start 2>/dev/null || true

echo "Building container image..."
podman rm -f openapi-demos 2>/dev/null || true
podman rmi -f openapi-demos 2>/dev/null || true

cd "$SCRIPT_DIR"
podman build --no-cache -t openapi-demos -f Dockerfile \
    --build-arg HELLO_STAGE="$HELLO_STAGE" .

echo "Starting container..."
podman run -d --name openapi-demos -p 8080:8080 \
    -e PROJECT_STAGE="$HELLO_STAGE" \
    openapi-demos

echo ""
echo "Waiting for WildFly to start..."
sleep 15

echo ""
echo "=== Demos running on http://localhost:8080 ==="
echo ""
echo "  Hello API (runtime: $HELLO_STAGE):"
echo "    REST:    http://localhost:8080/hello-api/hello"
echo "    OpenAPI: http://localhost:8080/hello-api/openapi"
if [ "$HELLO_STAGE" = "development" ]; then
    echo "    UI:      http://localhost:8080/hello-api/openapi-ui/"
else
    echo "    UI:      disabled (production)"
fi
echo ""
echo "  Greeting API (buildtime: ${GREETING_PROFILE:-production}):"
echo "    REST:    http://localhost:8080/greeting-api/greeting"
echo "    OpenAPI: http://localhost:8080/greeting-api/openapi"
if [ -n "$GREETING_PROFILE" ]; then
    echo "    UI:      http://localhost:8080/greeting-api/openapi-ui/"
else
    echo "    UI:      disabled (production)"
fi
echo ""
echo "  Info API (always enabled):"
echo "    REST:    http://localhost:8080/info-api/info"
echo "    OpenAPI: http://localhost:8080/info-api/openapi"
echo "    UI:      http://localhost:8080/info-api/openapi-ui/"
echo ""
echo "Stop with: podman stop openapi-demos && podman rm openapi-demos"
