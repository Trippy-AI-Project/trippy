
# GitHub Configuration

This directory contains configuration files for development tools and CI/CD.

## Directory Structure

```
.github/
├── checkstyle/          # Checkstyle static analysis configuration
│   ├── checkstyle.xml   # Main Checkstyle rules
│   ├── suppressions.xml # Rule suppressions for specific patterns
└── README.md            # This file
```

## Static Analysis Tools

### Checkstyle
- **Location**: `.github/checkstyle/`
- **Purpose**: Enforce Java coding standards
- **Configuration**: See below for details

### SpotBugs
- **Configuration**: Defined in parent `pom.xml`
- **Purpose**: Find potential bugs in Java code

### PMD
- **Configuration**: Defined in parent `pom.xml`
- **Purpose**: Source code analyzer for Java
- **Version**: 7.7.0 (supports Java 21)

## Checkstyle Configuration

This directory contains the Checkstyle configuration for the Trippy project.

### Files
- **`checkstyle.xml`** - Main Checkstyle rules configuration
- **`suppressions.xml`** - Suppressions for specific file patterns

### Suppression Rules
The following files are granted more lenient checks:

#### Spring Boot Application Classes (`*Application.java`)
- ✅ **No `FinalParameters` requirement** - Spring Boot entry points don't need final parameters
- ✅ **No `HideUtilityClassConstructor` check** - These are entry points, not utility classes

#### Configuration Classes (`*Config.java`, `*Configuration.java`)
- ✅ **No `FinalParameters` requirement** - Configuration methods often need mutable parameters

### Usage
The Checkstyle plugin is configured in the parent `pom.xml` and runs automatically during builds.

To run Checkstyle manually:
```bash
./mvnw checkstyle:check
```

To run all static analysis checks:
```bash
./mvnw checkstyle:check spotbugs:check pmd:check
```

## Running Static Analysis

Run all checks on a specific service:
```bash
./mvnw -pl services/api-gateway -DskipTests checkstyle:check spotbugs:check pmd:check
```

Run all checks on all services:
```bash
./mvnw -DskipTests checkstyle:check spotbugs:check pmd:check
```

## Notes
- Configuration files are centralized in `.github/` to keep the root directory clean
- Spring Boot `*Application.java` files have lenient rules (see checkstyle/suppressions.xml)
- All static analysis plugins are configured in the parent `pom.xml`
