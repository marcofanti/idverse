# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Maven-based Java project (Java 21) with groupId `org.itnaf` and artifactId `idverse`.

## Build Commands

```bash
# Build the project
mvn clean install

# Compile only
mvn compile

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Run a single test method
mvn test -Dtest=ClassName#methodName

# Package the application
mvn package

# Skip tests during build
mvn clean install -DskipTests
```

## Project Structure

Standard Maven layout:
- `src/main/java/` - Main application source code
- `src/main/resources/` - Application resources (config files, etc.)
- `src/test/java/` - Test source code
- `pom.xml` - Maven project configuration

## Development Environment

- Java Version: 21
- Build Tool: Maven
- IDE: IntelliJ IDEA (configuration files present in `.idea/`)
