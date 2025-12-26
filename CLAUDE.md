# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Contact Manager application for managing contacts for greeting cards. Spring Boot 3.3.4 web application using Thymeleaf templates with Spring Security authentication.

## Technology Stack

- Java 21
- Spring Boot 3.3.4 (Spring Web, Thymeleaf, Actuator, Security)
- Maven build system
- Jakarta Mail API with Eclipse Angus implementation
- JSoup for HTML sanitization
- Apache Commons Text for escaping
- Docker deployment via Jib (multi-arch: amd64/arm64)

## Common Development Commands

### Build and Run
```bash
# Build the application
./mvnw clean package

# Run the application locally
./mvnw spring-boot:run

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=SecurityConfigTest

# Skip tests during build
./mvnw package -DskipTests
```

### Docker/Container
```bash
# Build multi-arch Docker image with Jib
./mvnw jib:build -Dimage=<registry>/<image-name>

# Build to Docker daemon
./mvnw jib:dockerBuild
```

### Versioning
```bash
# Set version programmatically (used in CI)
./mvnw versions:set -DnewVersion=1.2.3 -DgenerateBackupPoms=false
```

## Application Configuration

- **Port:** 8080 (configurable in `src/main/resources/application.properties`)
- **Main Class:** `name.saak.contactmanager.ContactManagerApplication`
- **Base Package:** `name.saak.contactmanager`
- **Default User:** admin / geheim (in-memory, configured in SecurityConfig)

## Architecture

### Package Structure
- `name.saak.contactmanager.config` - Security and application configuration
- `name.saak.contactmanager.controller` - Spring MVC controllers

### Security Architecture
All requests except static resources (`/css/**`, `/js/**`) require authentication. Form-based login with custom login page at `/login`. Security headers include:
- Content Security Policy (CSP): restrictive default-src 'none' policy
- HSTS with preload and includeSubDomains
- X-Frame-Options: DENY
- Referrer-Policy: NO_REFERRER
- CSRF protection using CookieCsrfTokenRepository

User authentication uses in-memory UserDetailsService with a single admin user (SecurityConfig:44-48).

### Frontend
Thymeleaf templates in `src/main/resources/templates/` with reusable fragments in `templates/fragments/`. Static assets (CSS/JS) served from `src/main/resources/static/`.

### File Upload
Max file size: 10MB (both per-file and per-request limits configured in application.properties).

## CI/CD

GitHub Actions workflow (`.github/workflows/release.yml`) triggers on version tags (`v*`):
1. Extracts version from git tag (strips leading 'v')
2. Sets POM version to match tag
3. Builds JAR artifact
4. Creates GitHub Release with JAR attachment
5. Builds and pushes multi-arch Docker image to Docker Hub via Jib

Image tags: `latest` and the version number (e.g., `1.2.3`).

## Testing

Spring Boot Test and Spring Security Test dependencies included. Test classes in `src/test/java/name/saak/contactmanager/`.
