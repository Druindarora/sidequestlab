# SideQuestLab Copilot Instructions

Use `AGENTS.md` as the source of truth for repository policy.

- Read the root `AGENTS.md` before making changes.
- When touching `apps/public-site/`, `frontend/`, or `backend/`, also follow the nested `AGENTS.md` in that area.
- Keep changes small, scoped, and aligned with the requested slice.
- Do not edit generated OpenAPI files in `frontend/src/app/api/**` by hand.
- Use `./scripts/check.sh` as the repository validation entry point unless the task explicitly says otherwise.

This file is intentionally minimal to avoid duplicating or contradicting the AGENTS instructions.
