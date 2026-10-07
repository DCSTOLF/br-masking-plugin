# BR Delphix Masking Plugin

## Choose the plugin JAR

The repository supports both Delphix Java runtimes:

| Delphix Continuous Compliance version | Java runtime | Plugin JAR |
| --- | --- | --- |
| Earlier than 2026.5.0.0 | Java 8 | `dist/BR-java8.jar` |
| 2026.5.0.0 and later | Java 17 | `dist/BR-java17.jar` |

Import the JAR matching the Java runtime of the Delphix environment. The
Java 17 JAR is not compatible with older environments that only support Java 8.

Valid CPFs are delegated with all 11 digits to `dlpx-core:Brazil CPF`. Valid
numeric and alphanumeric CNPJs are delegated with all 14 characters to
`dlpx-core:Brazil CNPJ`, without punctuation. Invalid documents are rejected
before masking.

## Build

Build both JARs with JDK 17:

```sh
./gradlew clean assemble
```

The build uses Java 17 for the current artifact and Java 17's `--release 8`
option for the legacy artifact. Both JARs are written to `dist/`. The Java 8
build uses `masking-extensibility-api-1.5.0.jar` if it is present in `libs/`;
otherwise it falls back to the available API JAR and prints a warning. For the
most accurate legacy build, include the API JAR from the older Delphix release.
The legacy JAR advertises Extensibility API `1.5.0`; the Java 17 JAR advertises
`2026.5.0-FINAL`.

The Delphix Extensibility API and `semantic-version-2.1.0.jar` must be placed
in `libs/`. The Java 17 API JAR filename must match `maskingExtensibilityVer`
in `gradle.properties`.

Commit updated `dist/BR-java8.jar` and `dist/BR-java17.jar` after rebuilding
when plugin code or dependencies change.
