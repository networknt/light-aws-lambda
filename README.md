# light-aws-lambda
Aws lambda authorizer, middleware handlers and extensions for cross-cutting concerns

### authorizer

The authorizer is a lambda function and it will be configured on the AWS API Gateway to be invoked as a custom authorizer.


### middleware handlers

Middleware handlers are called by the framework before the Lambda function handlers are called to do the following.

##### JWT scope verifier

The custome authorizer on the API Gateway will enrich the request context with client_id/cid, user_id/uid (authorization flow), primary scopes, secondary scopes. The authorizer doesn't have the knowledge of OpenAPI specification on the API Gateway so the scope verification will be done on this middleware handler based on the openapi.yml specification.


##### Schema Validatior

After the JWT scope verifier, the same openapi.yaml will be leveraged to validate the request with json-schema-validator.

### lambda extensions


##### Logger

This is a Lambda extersion that will collect the logs of the Lambda function and send to Splunk or Elasticsearch.

##### Metrics

This is a Lambda extension that will collect the metrics of the Lambda function and send to a time series database.

### YAML configuration migration

`env-config` uses SnakeYAML Engine with YAML 1.2 Core resolution. See the
[configuration migration notes](env-config/README.md) for scalar changes,
duplicate-key handling, and dependency compatibility before rollout.

## Jackson 3 migration

This source line requires Jackson 3.2.3, json-schema-validator 3.0.8 and the Jackson 3 light-4j/http-client prerequisites. Public mapper and JsonNode types use tools.jackson; annotations retain com.fasterxml.jackson.annotation. Build those prerequisites before this reactor. Do not mix published Jackson 2 snapshot binaries with this source line.

Mappers use the shared Jackson2Defaults profile and preserve existing per-mapper overrides. Mappers are immutable; Java time and Optional support are built into Jackson 3. Classic SnakeYAML compatibility pins remain until the stack-wide cleanup.

Local qualification covers JVM tests and sanitized API Gateway/event and validation parity. It does not qualify deployed AWS execution or GraalVM native images.

### Reproducible Jackson 3 source build

`.github/workflows/jackson3-source-build.yml` builds the prerequisite source stack in an isolated Maven repository, reuses independently qualified full-core test evidence, and runs the prerequisite and full consumer suites. It pins reviewed policy `f0d2100f972eec765b2b276e7ace38922394f0a6` and candidate `bc3e4b5e2719d705d58a2edeabeb9bb5009c7401`; yaml-rule/http-client are selected from that verified evidence. The complete prerequisite closure is pinned in `ci/jackson3-source-refs.json`, initialized from the successful source-build manifest, with reviewed updates expressed as full SHAs; no default-branch or migration-branch lookup is used. Explicit `source_refs` replay overrides are merged into that map and remain subject to core qualification. The workflow uploads `source-refs.json`, `source-commits.txt`, `core-qualification.json` and Surefire reports. Core registration and workflow-token acceptance are merged in light-4j #2825 and #2826.

The optional `source_refs` workflow-dispatch input replays recorded full SHAs. GitHub enables dispatch only after the workflow exists on the default branch; before merge, reproduce from the recorded source checkouts locally. A green local suite is separate from a successful remote source-build run.

### Exact-source CI qualification

Consumer CI requires retained successful full-reactor evidence for the actual checked-out light-4j SHA and the same yaml-rule/http-client source commits. Core PR CI explicitly checks out the PR head, not its merge ref. Replay dispatches cannot qualify consumers. The attempt-specific evidence artifact records the actual checkout, prerequisite SHAs and test totals. A later pending, failed or cancelled run does not invalidate earlier valid evidence. All API pages and retained attempts are considered; missing or expired evidence blocks reuse. `core-qualification.json` records the accepted run and artifact.

The full dependency ledger is audited across all groups. Every NetworkNT release and snapshot must be source-installed, apart from the explicitly SHA-256-pinned Jackson 3 validator release. Known Jackson 2 yaml-rule and chaos-monkey releases are rejected even with a local-install marker. Jackson 2 dependencies in other groups are reported; ready migrations reject them except shared annotations.
