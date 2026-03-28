#!/usr/bin/env bash
# =============================================================================
# Trippy Platform - Infrastructure Setup & Verification Script
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { printf "${GREEN}[INFO]${NC}  %s\n" "$1"; }
warn()  { printf "${YELLOW}[WARN]${NC}  %s\n" "$1"; }
error() { printf "${RED}[ERROR]${NC} %s\n" "$1"; }

# ---- Prerequisites ----------------------------------------------------------
info "Checking prerequisites..."

if ! command -v docker &>/dev/null; then
    error "Docker is not installed. Please install Docker Desktop first."
    exit 1
fi

if ! docker info &>/dev/null; then
    error "Docker daemon is not running. Please start Docker Desktop."
    exit 1
fi

if ! docker compose version &>/dev/null 2>&1; then
    error "docker compose (v2 plugin) is not available."
    exit 1
fi

info "Docker $(docker --version | awk '{print $3}') detected."

# ---- .env setup -------------------------------------------------------------
if [ ! -f .env ]; then
    warn ".env file not found — creating from .env.example..."
    cp .env.example .env
    warn "IMPORTANT: Edit infra/docker/.env and replace all CHANGE_ME passwords before starting."
    warn "Run this script again after editing .env."
    exit 0
else
    info ".env file found."
fi

# Quick sanity: make sure placeholder passwords were changed
if grep -q "CHANGE_ME" .env; then
    warn "Your .env still contains CHANGE_ME placeholder passwords."
    warn "Please update all passwords in infra/docker/.env before running 'docker compose up'."
fi

# ---- Validate compose file --------------------------------------------------
info "Validating docker-compose.yaml..."
docker compose config --quiet 2>/dev/null && info "Compose file is valid." || {
    error "docker-compose.yaml validation failed. Run 'docker compose config' for details."
    exit 1
}

# ---- Summary -----------------------------------------------------------------
echo ""
info "=============================="
info "  Infrastructure is ready!"
info "=============================="
echo ""
echo "  Start all services:   docker compose up -d"
echo "  View live logs:        docker compose logs -f"
echo "  Check health:          docker compose ps"
echo "  Stop services:         docker compose down"
echo ""
echo "  Service URLs (after start):"
echo "    PostgreSQL:   localhost:5432"
echo "    Redis:        localhost:6379"
echo "    RabbitMQ UI:  http://localhost:15672"
echo "    SonarQube:    http://localhost:9000"
echo ""
