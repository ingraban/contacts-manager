#!/bin/bash

# Contact Manager - Java Start Script
# Dieses Skript startet die Anwendung direkt mit Java und lädt Umgebungsvariablen aus .env

set -e

# Farbcodes für Output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# JAR Datei
JAR_FILE="target/app.jar"

# Prüfe ob JAR existiert
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}Fehler: JAR-Datei nicht gefunden: $JAR_FILE${NC}"
    echo -e "${YELLOW}Bitte zuerst mit './mvnw clean package' bauen${NC}"
    exit 1
fi

# Lade .env Datei falls vorhanden
if [ -f .env ]; then
    echo -e "${GREEN}Lade Umgebungsvariablen aus .env Datei...${NC}"
    # Exportiere alle Variablen aus .env (ignoriere Kommentare und leere Zeilen)
    set -a
    source .env
    set +a
else
    echo -e "${YELLOW}Warnung: Keine .env Datei gefunden${NC}"
    echo -e "${YELLOW}Erstelle eine .env Datei basierend auf .env.example${NC}"
fi

# Setze Default-Werte falls nicht in .env gesetzt
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILE:-prod}"
export DATA_DIR="${DATA_DIR:-$(pwd)/data}"
export BACKUP_DIR="${BACKUP_DIR:-$(pwd)/backup}"

# Erstelle Verzeichnisse falls nicht vorhanden
mkdir -p "$DATA_DIR"
mkdir -p "$BACKUP_DIR"

# ==============================================
# ANWENDUNG STARTEN
# ==============================================

echo -e "${GREEN}Starte Contact Manager...${NC}"
echo -e "JAR-Datei: ${YELLOW}$JAR_FILE${NC}"
echo -e "Spring Profile: ${YELLOW}$SPRING_PROFILES_ACTIVE${NC}"
echo -e "Data Directory: ${YELLOW}$DATA_DIR${NC}"
echo -e "Backup Directory: ${YELLOW}$BACKUP_DIR${NC}"
echo -e "Base URL: ${YELLOW}${BASE_URL:-nicht gesetzt}${NC}"
echo ""

# Starte die Anwendung
java -jar "$JAR_FILE"
