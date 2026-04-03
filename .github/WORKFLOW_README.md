
# GitHub Configuration

This directory contains configuration files for CI/CD.

## Directory Structure

```
.github/
├── workflows/
│   └── ci.yaml          # CI pipeline (build & test only)
└── WORKFLOW_README.md   # This file
```

## CI Pipeline (`workflows/ci.yaml`)

The pipeline runs on every push and pull request targeting `dev` or `prod`.

### Jobs

| Job | Purpose |
|-----|---------|
| `check-no-env-files` | Blocks any `.env` files from being committed (`.env.example` / `.env.template` are allowed) |
| `detect-changes` | Uses `dorny/paths-filter` to determine which services have changed |
| `build-parent` | Validates and installs the root POM |
| `build-<service>` | Builds and runs tests for each changed service only |
| `ci-status` | Final gate — fails the PR if any job above failed |

### What the CI checks

- **Build**: `mvn clean verify` per changed service
- **Tests**: All unit/integration tests (`-DskipTests=false`)
- **No `.env` files**: Any tracked `.env` file (other than `.env.example`/`.env.template`) will fail the pipeline

### What the CI does NOT check

Static analysis tools (Checkstyle, SpotBugs, PMD) have been removed from the pipeline. Run them locally if needed:

```bash
./mvnw -pl services/<service> -DskipTests pmd:check
```

## Notes
- CodeRabbit automated reviews are disabled via `.coderabbit.yaml` at the repo root.
- Concurrency is configured so a newer run on the same branch cancels any in-progress run.
- Service jobs are skipped (not failed) when no relevant files changed, keeping PRs fast.
