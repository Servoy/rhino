# Project Context — Servoy Rhino / DLTK JavaScript engine

This repo is **`org.eclipse.dltk.javascript.rhino`** — Servoy's fork of the Mozilla Rhino
JavaScript engine (MPL 2.0), built as a single Eclipse/OSGi bundle and used as the scripting
engine by the Servoy client and developer. It is NOT the Servoy Developer IDE.

## SDD variant

This repo uses the **sdd-java-eclipse** shared skill (Java / Eclipse-OSGi pipeline): built with
Maven + Tycho, packaged as an `eclipse-plugin`, dependencies/exports via `META-INF/MANIFEST.MF`.

## Stack (facts an SDD run needs)

| Aspect | Value |
|--------|-------|
| Java version | 21 (Servoy fork uses Java 16+ features; upstream gradle's `release=11` does not apply) |
| Build | Maven (`mvn`) — NOT the upstream gradle build |
| Packaging | `eclipse-plugin` (single OSGi bundle `org.eclipse.dltk.javascript.rhino`) |
| Tests | JUnit 5, run via the Eclipse JUnit runner (`eclipse-ide_runClassTests` / `runAllTests`) |

## Read AGENTS.md first

The repo-root `AGENTS.md` is authoritative and detailed — read it before working. It covers:
the Maven-not-gradle build, the Eclipse JUnit test runner, module layout (`rhino`, `rhino-tools`,
`tests`), spotless formatting after every change, JUnit-5-for-new-tests, the Rhino architecture
(TokenStream → Parser → IRFactory → Codegen/Interpreter), and the **Bundle-Version bump rule**
(qualifier bump once per Servoy release; keep `pom.xml` and `META-INF/MANIFEST.MF` in sync).

## SDD-relevant notes

- Public API = the bundle's `Export-Package`; changing it affects `servoy_shared` and the client.
- Mostly upstream Rhino code — keep Servoy patches surgical; a huge diff on an upstream file
  usually means unintended reformatting. Let spotless handle formatting.
- New tests go in `rhino` or `tests` (decide per case by looking at existing tests), JUnit 5.
