# Elasticsearch Java Client 9 Migration Assessment

Source request: migrate `co.elastic.clients:elasticsearch-java` from `8.18.8` to
`9.5.4`.

## Current repository state

No migration was applied in this slice because the backend does not currently
declare or use `co.elastic.clients:elasticsearch-java`.

Verified checks:

- `backend/pom.xml` has no `co.elastic.clients:elasticsearch-java` dependency.
- `git grep` found no checked-in references to `co.elastic.clients`,
  `elasticsearch-java`, `ElasticsearchClient`, `Rest5Client`, or
  `elasticsearch-rest-client`.
- The backend is already on Java 21, so the 9.x client Java 17 minimum is not a
  blocker if the dependency is reintroduced later.

## Breaking changes identified

Elastic's 9.0.0 Java client release notes list these migration-relevant changes:

- Java baseline changes from Java 8 to Java 17.
- The legacy `org.elasticsearch.client:elasticsearch-rest-client` dependency is
  now optional because 9.x introduces `Rest5Client`.
- Projects that still instantiate the legacy `RestClient` must either add the
  `elasticsearch-rest-client` dependency explicitly or migrate transport setup
  to `Rest5Client`.
- Several generated API types changed shape, including nullable aggregation
  numeric values, script source builders, `Hit.matchedQueries()` changing from a
  list-like access pattern to map semantics, response body getter renames,
  `indicesBoost`/`dynamicTemplates` using `NamedValue`, and several enum/object
  type corrections.
- Deprecated request/model classes were removed for APIs no longer accepted by
  Elasticsearch 9.

Known 9.x issues relevant to choosing a target:

- Some early 9.x `Rest5Client` releases had memory and latency regressions.
  Elastic marks these fixed by the later 9.2.9, 9.3.9, and 9.4.5 patch lines.
- `9.5.4` is beyond those affected ranges, so it is the preferred target if a
  future slice reintroduces this client.

## Proposed migration plan if Elasticsearch usage is restored

1. Add only `co.elastic.clients:elasticsearch-java:9.5.4` to `backend/pom.xml`.
2. Prefer `Rest5Client`/`ElasticsearchClient.of(...)` for new transport setup.
   Add `org.elasticsearch.client:elasticsearch-rest-client:9.5.4` only if
   existing code still requires the legacy low-level client.
3. Compile and fix any generated API breakages called out above, with special
   attention to null aggregation values and script-source builders.
4. Add focused unit tests around any repository/service code that builds queries
   or reads search responses.
5. Run `./scripts/check.sh`.

## References

- Elastic Java client 9.0.0 release notes:
  https://raw.githubusercontent.com/elastic/elasticsearch-java/main/docs/release-notes/9-0-0.md
- Elastic Java client known issues:
  https://raw.githubusercontent.com/elastic/elasticsearch-java/main/docs/release-notes/known-issues.md
