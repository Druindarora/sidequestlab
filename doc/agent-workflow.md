# Agent Workflow (V0) — Comment travailler dans ce repo

## But

Livrer une PR petite, vérifiable, CI verte, avec un minimum de churn.

## Branching

- Ne jamais commit directement sur `main` (stable) ni sur `dev` (intégration).
- Travailler sur `work/<sujet>` (ex: `work/slice-session-db`).
- Le humain (toi) review et merge.

## Boucle de travail standard (obligatoire)

1. **Plan (5 bullets max)** :
   - fichiers/dossiers impactés
   - ce qui est ajouté/modifié
   - tests/checks à exécuter
2. **Changements minimaux** :
   - pas de refacto hors scope
   - pas de renommage massif
3. **Exécuter le check** :
   - `./scripts/check.sh`
4. **Si rouge → corriger** :
   - corriger la cause la plus proche
   - relancer `./scripts/check.sh`
   - répéter jusqu’au vert
5. **Préparer la PR** :
   - résumé (quoi/pourquoi)
   - liste des fichiers touchés
   - commandes exécutées + résultat
   - points à tester manuellement (si UI)

## PR template (copier/coller)

Objectif :

Changements :

Checks exécutés :
- `./scripts/check.sh` — OK

Tests manuels :
- N/A

## Politique OpenAPI

- Ne pas modifier `frontend/src/app/api/**` à la main.
- Si l’API change et qu’une regen est requise :
  - lancer la regen côté front (nécessite backend up)
  - puis valider avec :
    - `ALLOW_GENERATED_API_CHANGES=1 ./scripts/check.sh`

## “Definition of Done” (PR acceptable)

- `./scripts/check.sh` vert
- PR petite et focalisée (un sujet)
- docs/état mis à jour si nouvelle feature (2–10 lignes max dans `doc/project-context.md`)
- aucun changement interdit (infra/.github/api généré hors regen)
