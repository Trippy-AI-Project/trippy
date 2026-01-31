# Git Branch & Commit Guidelines

## Branch naming
Use lowercase and keep it short. Include a issue ID when you have one.

- `feature-FE/<short-feature>` (e.g., `feature-FE/login-form`)
- `feature-BE/<short-feature>` (e.g., `feature-BE/user-profile-api`)
- `story/<id>-<short-story>` (e.g., `story/1234-password-reset`)
- `backlog/<id>-<short-item>` (e.g., `backlog/987-cleanup-auth`) 
- `bugfix/<id>-<short-bug>` (e.g., `bugfix/456-null-check`)
- `hotfix/<id>-<short-fix>` (e.g., `hotfix/789-prod-timeout`)
- `chore/<short-task>` (e.g., `chore/update-deps`)
- `docs/<short-topic>` (e.g., `docs/api-setup`)
- `spike/<short-topic>` (e.g., `spike/cache-strategy`)
- `release/<version>` (e.g., `release/1.4.0`)

## Commit best practices
- keep Commits small and reviewable.
- Use present tense and be specific (good: "add login validation").
- Optional but recommended format: `<type>: <summary>` (types: feat, fix, chore, docs, refactor, test).
- Reference the ticket ID in the commit if you used one in the branch name.

Following these practices helps future you and the team debug faster, improve features confidently, and build credibility as a deliberate developer. ⚡️