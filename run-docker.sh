#!/bin/bash

# Contact Manager - Docker Start Script
# Dieses Skript startet die Anwendung als Docker Container mit allen notwendigen Konfigurationen

set -e

# Farbcodes für Output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Container Name
CONTAINER_NAME="contact-manager"

# Image Name (Java 21 Runtime)
IMAGE_NAME="eclipse-temurin:21-jre"

# JAR Datei
JAR_FILE="/Users/saak/git/Privat/contacts-manager/target/app.jar"

# Lade .env Datei falls vorhanden
if [ -f .env ]; then
    echo -e "${GREEN}Lade Umgebungsvariablen aus .env Datei...${NC}"
    export $(grep -v '^#' .env | xargs)
fi

# Prüfe ob JAR existiert
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}Fehler: JAR-Datei nicht gefunden: $JAR_FILE${NC}"
    echo -e "${YELLOW}Bitte zuerst mit './mvnw clean package' bauen${NC}"
    exit 1
fi

# ==============================================
# UMGEBUNGSVARIABLEN - ANPASSEN!
# ==============================================

# OAuth2 Konfiguration (Gitea)
OAUTH2_CLIENT_ID="${OAUTH2_CLIENT_ID:-your-client-id}"
OAUTH2_CLIENT_SECRET="${OAUTH2_CLIENT_SECRET:-your-client-secret}"

# Base URL der Anwendung (für OAuth2 Redirect)
BASE_URL="${BASE_URL:-http://localhost:8080}"

# Datenbank Passwort (Format: <db-password> <encryption-password>)
# WICHTIG: Für Production ändern!
#DB_PASSWORD="${DB_PASSWORD:-changeme change-encryption-key}"

# Spring Profile (prod für Production)
#SPRING_PROFILE="${SPRING_PROFILE:-prod}"

# ==============================================
# VOLUMES FÜR PERSISTENTE DATEN
# ==============================================

# Verzeichnis für Datenbank-Dateien
DATA_DIR="${DATA_DIR:-$(pwd)/data}"

# Verzeichnis für Backups
BACKUP_DIR="${BACKUP_DIR:-$(pwd)/backup}"

# Erstelle Verzeichnisse falls nicht vorhanden
mkdir -p "$DATA_DIR"
mkdir -p "$BACKUP_DIR"

# ==============================================
# CONTAINER MANAGEMENT
# ==============================================

# Stoppe und entferne alten Container falls vorhanden
if [ "$(docker ps -aq -f name=$CONTAINER_NAME)" ]; then
    echo -e "${YELLOW}Stoppe existierenden Container...${NC}"
    docker stop $CONTAINER_NAME 2>/dev/null || true
    docker rm $CONTAINER_NAME 2>/dev/null || true
fi

# ==============================================
# CONTAINER STARTEN
# ==============================================

echo -e "${GREEN}Starte Contact Manager Container...${NC}"
echo -e "Container Name: ${YELLOW}$CONTAINER_NAME${NC}"
echo -e "Spring Profile: ${YELLOW}$SPRING_PROFILE${NC}"
echo -e "Data Directory: ${YELLOW}$DATA_DIR${NC}"
echo -e "Backup Directory: ${YELLOW}$BACKUP_DIR${NC}"
echo -e "Base URL: ${YELLOW}$BASE_URL${NC}"
echo ""

docker run -d \
  --name $CONTAINER_NAME \
  --restart unless-stopped \
  -p 8080:8080 \
  -v "$(pwd)/$JAR_FILE:/app/app.jar:ro" \
  -v "$DATA_DIR:/data" \
  -v "$BACKUP_DIR:/backup" \
  -e SPRING_PROFILES_ACTIVE=$SPRING_PROFILE \
  -e OAUTH2_CLIENT_ID="$OAUTH2_CLIENT_ID" \
  -e OAUTH2_CLIENT_SECRET="$OAUTH2_CLIENT_SECRET" \
  -e BASE_URL="$BASE_URL" \
  -e SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD" \
  $IMAGE_NAME \
  java -jar /app/app.jar

# Prüfe ob Container läuft
sleep 2
if [ "$(docker ps -q -f name=$CONTAINER_NAME)" ]; then
    echo -e "${GREEN}✓ Container erfolgreich gestartet${NC}"
    echo ""
    echo -e "Anwendung erreichbar unter: ${GREEN}http://localhost:8080${NC}"
    echo ""
    echo "Nützliche Befehle:"
    echo "  docker logs -f $CONTAINER_NAME    # Logs anzeigen"
    echo "  docker stop $CONTAINER_NAME       # Container stoppen"
    echo "  docker start $CONTAINER_NAME      # Container starten"
    echo "  docker restart $CONTAINER_NAME    # Container neustarten"
else
    echo -e "${RED}✗ Fehler beim Starten des Containers${NC}"
    echo "Logs:"
    docker logs $CONTAINER_NAME
    exit 1
fi
