# CLAUDE.md — Office Document Templating Connector

## Project Overview

**Artifact:** `org.bonitasoft.connectors:bonita-connector-document-templating`
**Current version:** 3.0.0-SNAPSHOT
**License:** GPL v2
**Description:** A Bonita connector that merges data into `.docx` (Word) and `.odt` (LibreOffice) document templates using the [Apache Velocity](http://velocity.apache.org/) templating engine via [XDocReport](https://github.com/opensagres/xdocreport).

The connector accepts a Bonita process document as a template and a list of key/value replacements, renders the template, sanitizes the output (strips illegal XML 1.0 characters), and returns a `DocumentValue` that Bonita stores back into the process.

**Connector definition ID:** `document-templating` (version `1.0.1`)
**Main class:** `org.bonitasoft.connectors.document.templating.DocumentTemplating`
**Bonita engine compatibility:** 10.0.0+, Java 17

---

## Build Commands

The project uses the **Maven Wrapper** (`mvnw`). Prefer the wrapper over a locally installed Maven.

```bash
# Full build (compile + test + package + JaCoCo coverage report)
./mvnw

# Compile only
./mvnw compile

# Run unit tests only
./mvnw test

# Package (produces JAR + assembly ZIP) without running tests
./mvnw package -DskipTests

# Verify (same as default goal — runs tests and coverage)
./mvnw verify

# SonarCloud analysis (CI only — requires SONAR_TOKEN)
./mvnw sonar:sonar

# Deploy to Maven Central (requires GPG key + Central credentials)
./mvnw deploy -P deploy
```

**Build outputs** (in `target/`):
- `bonita-connector-document-templating-<version>.jar` — connector JAR
- `bonita-connector-document-templating-<version>-all.zip` — full assembly including all dependencies (for manual installation in Bonita Studio)
- `bonita-connector-document-templating-<version>-sources.jar`
- `bonita-connector-document-templating-<version>-javadoc.jar`
- `site/jacoco/` — JaCoCo HTML coverage report

---

## Architecture

```
src/
├── assembly/                          Maven Assembly descriptors
│   ├── all-assembly.xml               Bundles connector + all runtime deps
│   └── document-templating-assembly.xml
├── main/
│   ├── java/org/bonitasoft/connectors/document/templating/
│   │   ├── DocumentTemplating.java    Main connector — extends AbstractConnector
│   │   └── ZipUtil.java               ZIP pack/unpack utility (used for .docx/.odt internals)
│   ├── resources/                     I18n properties + icons
│   └── resources-filtered/
│       ├── document-templating.def    Connector definition XML (Maven-filtered)
│       └── document-templating.impl   Connector implementation descriptor (Maven-filtered)
├── script/
│   └── dependencies-as-var.groovy    Generates dependency list as a resource variable at build time
└── test/
    ├── java/…/DocumentTemplatingTest.java
    ├── java/…/ZipUtilTest.java
    └── resources/                     .docx/.odt fixtures, corrupted XML samples
```

### Key classes

| Class | Responsibility |
|---|---|
| `DocumentTemplating` | Connector entry point. Validates inputs, fetches the Bonita document, invokes XDocReport/Velocity to render the template, sanitizes illegal XML chars, returns a `DocumentValue`. |
| `ZipUtil` | Wraps `java.util.zip` — unzips a `.docx`/`.odt` archive to a temp directory and re-zips it after sanitization. Normalises path separators for cross-platform compatibility. |

### Connector inputs / outputs

| Parameter | Type | Mandatory | Description |
|---|---|---|---|
| `documentInput` | `String` | Yes | Name of the Bonita process document used as template (`.docx` or `.odt`) |
| `replacements` | `List<List<Object>>` | Yes | List of `[key, value]` pairs injected into the Velocity context |
| `outputFileName` | `String` | No | Override the output document filename; defaults to the template filename |
| `document` _(output)_ | `DocumentValue` | — | Rendered document bytes + MIME type + filename |

### Template engine

Templates use **Apache Velocity** syntax. Variables are referenced as `${variableName}` or `${object.property}`. A `sorter` (`SortTool`) is pre-loaded in the context for list sorting. The connector is tested with both `.docx` (Word) and `.odt` (LibreOffice) formats.

---

## Testing

Tests are in `src/test/java/…` and run with **JUnit 5 + Mockito + AssertJ**.

```bash
# Run all unit tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=DocumentTemplatingTest

# Run a single test method
./mvnw test -Dtest=DocumentTemplatingTest#process_docx_document
```

**Test fixtures** (`src/test/resources/`):

| File | Purpose |
|---|---|
| `velocitytest.docx` | Full Velocity template with fields, object properties, lists, and sort |
| `template.docx` / `template.odt` | Templates with an embedded invalid XML 1.0 character (used for sanitization tests) |
| `unzipTest.docx` | ZIP/unzip round-trip test |
| `corrupted.xml` / `notCorrupted.xml` | Raw XML files for `isCorrupted()` unit test |

**Coverage:** JaCoCo is configured to run during `verify`. The HTML report is generated at `target/site/jacoco/index.html`. SonarCloud gate is configured at `sonar.projectKey=bonitasoft_bonita-connector-document-templating`.

---

## Commit Format

This repository follows **Conventional Commits**. The `commit-message-check.yml` workflow enforces the format on all PRs.

```
<type>(<scope>): <short summary>

[optional body]

[optional footer(s)]
```

**Allowed types:** `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `chore`, `ci`, `build`, `revert`

Examples:
```
feat(connector): support .docx templates with nested Velocity loops
fix(sanitize): remove null chars from ODT content.xml before re-zipping
chore(deps): bump commons-text from 1.11.0 to 1.12.0
ci: add Claude Code review workflow
```

---

## Release Process

Releases are handled via the `release.yml` GitHub Actions workflow (reusable workflow from `bonitasoft/github-workflows`).

1. On the release branch, update `pom.xml` version — remove `-SNAPSHOT` suffix.
2. Run the [**Create release** action](https://github.com/bonitasoft/bonita-connector-document-templating/actions/workflows/release.yml) with the target version (e.g. `3.0.0`).
3. The workflow: creates a Git tag, publishes to Maven Central (via `central-publishing-maven-plugin`), and creates a GitHub Release.
4. Merge back to `master` and bump to the next `-SNAPSHOT` version.
5. Update the [Bonita marketplace repository](https://github.com/bonitasoft/bonita-marketplace) with the new version.

**Signing:** Artifacts are GPG-signed using the `deploy` Maven profile. The CI uses `MAVEN_GPG_PRIVATE_KEY` and `MAVEN_GPG_PASSPHRASE` secrets. The Central publishing plugin is configured with `autoPublish: true`.
