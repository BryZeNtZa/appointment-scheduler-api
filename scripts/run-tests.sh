#!/usr/bin/env bash
# Runs the full test suite (unit + Testcontainers integration) inside a Maven
# container, so no JDK/Maven is required on the host - only Docker.
#
# The Docker socket is mounted so Testcontainers can start a sibling Postgres
# container. DOCKER_API_VERSION/api.version are pinned because the bundled
# docker-java client cannot negotiate the API version of Docker Engine >= 29.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "Running tests in a Maven container (no host JDK needed)."
echo "First run downloads dependencies and images - this can take a few minutes. Progress streams live below."

# Pre-pull the Postgres image on the host (with a visible progress bar) when it
# is not cached. Testcontainers shares the host daemon via the mounted socket,
# so it then reuses this image instead of pulling it silently in-container.
if ! docker image inspect postgres:16-alpine >/dev/null 2>&1; then
  echo "Pulling postgres:16-alpine ..."
  docker pull postgres:16-alpine
fi

# Interactive console: allocate a TTY (-t) so output streams live, and omit
# Maven batch mode (-B) so Maven shows its live download/transfer progress meter.
# Redirected/CI: keep -B for clean logs and drop -t (which would hang).
tty_flag=()
mvn_batch=(-B)
if [ -t 1 ]; then tty_flag=(-t); mvn_batch=(); fi

docker run --rm "${tty_flag[@]}" \
  -v "${REPO_ROOT}:/app" \
  -v afb_m2:/root/.m2 \
  -v "/var/run/docker.sock:/var/run/docker.sock" \
  -e DOCKER_HOST=unix:///var/run/docker.sock \
  -e DOCKER_API_VERSION=1.44 \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  -e TESTCONTAINERS_RYUK_DISABLED=true \
  --add-host=host.docker.internal:host-gateway \
  -w /app \
  maven:3.9-eclipse-temurin-21 mvn "${mvn_batch[@]}" test -DargLine="-Dapi.version=1.44"
