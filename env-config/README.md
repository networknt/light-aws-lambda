# YAML configuration

`Configuration` reads the classpath `app.yml` using SnakeYAML Engine 3.0.1 with
the YAML 1.2 Core schema. Its public methods and stage-to-map structure are unchanged.
The reader is local to initialization and the resource stream is closed after loading.
Duplicate keys retain the previous last-value-wins behavior.

This changes scalar resolution from classic SnakeYAML's YAML 1.1 defaults:

| Input | Classic SnakeYAML | Engine Core |
| --- | --- | --- |
| `on`, `off`, `yes`, `no` | Boolean | String |
| `0755` | Integer 493 | Integer 755 |
| `0o755` | String | Integer 493 |
| `0b11` | Integer 3 | String |
| `1_000` | Integer 1000 | String |
| `1:30` | Integer 90 | String |
| `1:30.5` | Float 90.5 | String |
| `1_0.5` | Float 10.5 | String |
| `2026-10-24` | Date | String |

Use `true`/`false` for booleans, ordinary decimal numbers or YAML 1.2 `0o` octal
notation for integers, and quote values that must remain strings. Explicit legacy
YAML 1.1 scalar tags, Java object tags, and merge-key behavior are not a compatibility
contract of this loader. Review existing `app.yml` files before rollout.

The direct classic SnakeYAML dependency is replaced by Engine. Consumers importing
`org.yaml.snakeyaml` through `env-config` must declare their own classic dependency.
Other light-4j dependencies may still bring classic SnakeYAML transitively; this
migration does not remove it from the entire stack.
The parent retains classic SnakeYAML 2.7 dependency management so those transitive
consumers do not silently fall back to older versions.

Project versions remain `2.4.2-SNAPSHOT` for the owner's coordinated release update.
The scalar and dependency changes require coordinated release review before rollout.
