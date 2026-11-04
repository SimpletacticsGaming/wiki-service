# Secrets and required environment variables

**Nothing in this list may be committed.** `.gitignore` blocks `*-secrets.properties`,
`*-secrets.txt` and `.env`; keep them out of the repo regardless.

## Required at runtime

| Variable | Purpose |
|---|---|
| `DB_CONNECTION_STRING` | JDBC URL, e.g. `jdbc:postgresql://localhost:5433/wiki` |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `WIKI_SERVICE_AUTH_TOKEN` | Shared secret `sita-backend` must send in the auth header |
| `SITA_BOT_ALERTING_URL` | Base URL of the sita-bot alerting endpoint |
| `SITA_BOT_ALERTING_CHANNEL_ID` | Discord channel id alerts are posted to |

`sita.bot.alerting.enabled=false` makes the last two optional.

## Local development

Create an untracked `application-secrets.properties` next to this file:

```properties
WIKI_SERVICE_AUTH_TOKEN=local-dev-token
SITA_BOT_ALERTING_CHANNEL_ID=
```

and run `./gradlew bootRunLocal` (activates the `local,secrets` profiles).
`application-local.properties` supplies the database values for the
`docker compose` database.

## Docker / compose

`docker-compose.yaml` reads `./sita-bot-secrets.txt` via `env_file`. Copy
`sita-bot-secrets.txt.example` to `sita-bot-secrets.txt` and fill in real values —
the real file is gitignored.

## If a secret leaks

Rotate first, scrub second. Deleting the file does not remove it from git
history; the commit still serves it to anyone with clone access.
