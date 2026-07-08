# Release and publish to Maven Central (Sonatype Central Portal)

This document describes how to produce a **local signed build** and, when you are ready, how to **publish** to Maven Central using the [Sonatype Central Portal](https://central.sonatype.com). The library is **not yet published**; consumers should use `mvn clean install` and `mavenLocal()` until a release is deployed.

---

## Prerequisites

- **Java 21** and **Maven** on the path.
- **GnuPG** installed and a default key for signing (e.g. `gpg --list-secret-keys`).
- For publishing: a [Sonatype Central](https://central.sonatype.com) account and a [user token](https://central.sonatype.org/publish/generate-portal-token/) configured in `~/.m2/settings.xml`.

### settings.xml (for publishing only)

Add a server with id `central` and your token (never commit this file):

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>YOUR_TOKEN_USERNAME</username>
      <password>YOUR_TOKEN_PASSWORD</password>
    </server>
  </servers>
</settings>
```

Use the username and password from the Central Portal when you generate the user token.

---

## 1. Local signed build (no publish)

From the **repository root**:

```bash
# Run tests
mvn clean verify -q

# Build and sign (main + sources + javadoc + .asc). Prompts for GPG passphrase unless gpg-agent is used.
mvn clean verify -Prelease -q
```

This creates in `notifier-pipe-core/target/` the main JAR, `-sources.jar`, `-javadoc.jar`, and GPG `.asc` files. Nothing is uploaded.

Optional: install into the local Maven repo:

```bash
mvn clean verify install -Prelease -q
```

---

## 2. Publish to Maven Central (Central Portal)

The **`release`** profile is **off by default**. Signing and Central Portal publishing only run when you activate it with `-Prelease`.

### Use a release version

Maven Central does not accept `-SNAPSHOT` for published releases. Set a release version (e.g. `1.0.0`) in both POMs before publishing:

- `pom.xml`: `<version>1.0.0</version>`
- `notifier-pipe-core/pom.xml`: no `<version>` (inherits from parent) or ensure it matches.

You can use the [versions-maven-plugin](https://www.mojohaus.org/versions-maven-plugin/) or edit the POMs manually.

### Exact Maven commands to publish

From the **repository root**:

```bash
# Clean, build, sign, and deploy to Central Portal (bundle upload + validation).
# Requires: release version in POMs, GPG key, and settings.xml with server id "central".
mvn clean deploy -Prelease -DskipTests
```

What this does:

- **`-Prelease`** — enables GPG signing (verify phase) and `central-publishing-maven-plugin`; produces `.asc` for the main JAR, sources JAR, javadoc JAR, and POM; then `deploy` builds a bundle and uploads it to the Central Portal for validation.
- **`-DskipTests`** — optional; skip tests during the release deploy.

After a successful run, the plugin prints a link to the Central Portal. Open it to confirm validation and, if you did not use `autoPublish`, click **Publish** to sync the component to Maven Central.

### Overriding plugin defaults (optional)

You can override configuration on the command line:

- **Wait until published (requires auto-publish):**  
  `-Dcentral.waitUntil=published -Dcentral.autoPublish=true`
- **Custom deployment name in the Portal:**  
  `-Dcentral.deploymentName=notifier-pipe-1.0.0`

---

## Summary

| Goal                    | Command                                      |
|-------------------------|----------------------------------------------|
| Local signed build      | `mvn clean verify -Prelease`                 |
| Install signed locally  | `mvn clean verify install -Prelease`         |
| Publish to Central      | `mvn clean deploy -Prelease -DskipTests`     |

Artifacts are signed and sources/javadoc JARs are attached; the `release` profile also enables the Central Portal plugin for `deploy`. The profile does not run by default.
