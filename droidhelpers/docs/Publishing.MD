# Publishing Guide
We are currently using gitpack.io, Maven Central will be considered later.
This doc describe how to publish on `jitpack.io`, `GitHub Packages` and `Maven Central` (later).

---

## GitPack.io
jitpack.io is a free service that allows you to publish your Gradle/Maven artifacts directly from GitHub repositories. 
It does not require complex setup or configuration, few steps are needed:
1. Make sure your project is public on GitHub.
2. Make sure your project has a valid `build.gradle` file with the correct `group`, `version`, and `artifactId` settings.
3. add `jitack.yml` file in the root of your project with the following content:
```yaml
jdk: openjdk11
```
4. Commit and push your changes to GitHub. 
5. Go to https://jitpack.io and enter your GitHub repository URL in the search bar.
6. Click on the "Get it" button and select the version you want to publish.
7. Copy the Gradle/Maven dependency snippet provided by JitPack and add it to your project's `build.gradle` file.
8. Make sure to include the JitPack repository in your `build.gradle` file:
```groovy 
repositories {
    maven { url 'https://jitpack.io' }
}
```

## GitHub Packages 

### How repo naming works, module name vs. parent project name

This trips people up, so it's worth being explicit:

| Term | What it is                                                                          | Where it matters                                                                                                                                                                                                                                                                                     |
|---|-------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **GitHub repository name** | The literal name of the repo on github.com, e.g. `github.com/username/droidhelpers` | Must **exactly match** the path segment in the `maven.pkg.github.com/{owner}/{repo}` URL in `build.gradle`. This is a hard GitHub requirement, the URL points at a real repo you have publish access to.                                                                                             |
| **Gradle `rootProject.name`** (in `settings.gradle`, if this module ever sits inside a larger multi-module workspace) | An internal Gradle build identifier                                                 | Purely local to your build. Never appears in the published Maven coordinates, never shown to consumers, never seen in the GitHub Packages UI. It can be anything, e.g. a workspace called `RochdiWafikAndroidLibraries` containing this module as a subproject, without affecting publishing at all. |
| **`artifactId`** (in the `publishing` block) | The published library's name, e.g. `droidhelpers`                                   | This is what consumers type in their `implementation '...'` line. Not required to match the repo name, but keeping them identical (as this project does) removes any guesswork for future maintainers and consumers.                                                                                 |

**Bottom line:** the current `build.gradle` hardcodes the publish target to `username/droidhelpers`, so the GitHub repository must be named exactly `droidhelpers` under the `rochdi-wafik` account/org. If you ever rename the repo, update the `url = uri(...)` line below to match, or publishing will fail with a 404/403.

### Prerequisites

- A GitHub Personal Access Token (classic) with the `write:packages` scope (this includes `read:packages`). If the repo is private, also add `repo`.
- The token stored locally, **never commit it**:

  `~/.gradle/gradle.properties`
  ```properties
  gpr_user=your-github-username
  gpr_key=ghp_xxxxxxxxxxxxxxxxxxxx
  ```

### The `build.gradle` publishing config

This is what's already in the project, reproduced here for reference:

```groovy
android {
    // ...

    // Create the 'release' variant (with sources + javadoc jars) for publishing.
    publishing {
        singleVariant('release') {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

// --- GitHub Packages ---
// Publish with: ./gradlew publishReleasePublicationToGitHubPackagesRepository
// or:           ./gradlew publish   (publishes to every configured repository)
afterEvaluate {
    publishing {
        publications {
            release(MavenPublication) {
                from components.release
                groupId = 'com.github.rochdi-wafik'   // GitHub username/org
                artifactId = 'droidhelpers'    // Module name
                version = '1.0.0'                // Should match your Git tag
            }
        }
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/rochdi-wafik/droidhelpers")
                credentials {
                    username = gpr_user
                    password = gpr_key
                }
            }
        }
    }
}
```

`groupId = com.github.<owner>` is the GitHub Packages convention (not enforced by Gradle, but expected by consumers and by GitHub's own auto-generated install snippets).

### Steps to publish a new version

1. Make sure everything is merged into your default branch and the working tree is clean.
2. Bump the version in **two places** (they should always match):
    - `buildConfigField "String", "LIB_VERSION", "\"1.0.1\""` in `defaultConfig`
    - `version = '1.0.1'` in the `publishing` block
3. Tag the release in Git and create a matching GitHub Release (Releases → "Draft a new release" → pick the tag):
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```
4. Publish to GitHub Packages:
   ```bash
   ./gradlew publishReleasePublicationToGitHubPackagesRepository
   ```
5. Verify: go to the repo → **Packages** (right sidebar) → confirm the new version is listed.

### What the GitHub Package listing looks like

Once published, the repo's sidebar shows a **Packages** section linking to a package page with:
- Package name: `droidhelpers` (Maven type)
- A version history list (`1.0.0`, `1.0.1`, ...), each with its publish date and a link back to the exact commit/workflow run that published it
- An auto-generated "Installation" snippet (Gradle/Maven/sbt tabs), this is what `USAGE.md`'s installation section mirrors
- Visibility: **inherits from the repository** (public repo → publicly listed package)

**Important caveat, worth knowing before this goes fully open-source:** even when the repository and package are public, GitHub Packages' Maven registry still requires every consumer to authenticate with a Personal Access Token (`read:packages` scope) to actually *download* the artifact, this is a GitHub platform limitation, not something you can configure away. It's fine for now (internal use, or early adopters you're in direct contact with), but it means a stranger can't just add your repo and `implementation '...'`, they'd need to generate their own token first. This is the main reason Maven Central is worth revisiting once you're ready for frictionless public adoption (Maven Central has no such requirement).

---

## Maven Central (future)

Not covered in detail yet, flagged here as the next step once the SDK is ready for anonymous public consumption. At a high level it will require:
- A Sonatype/Central Portal namespace claim (typically a reverse-DNS domain you control, e.g. `io.github.rochdi-wafik` or a verified custom domain)
- GPG-signing every published artifact
- Stricter POM metadata (license, SCM URL, developer info) than GitHub Packages requires

We'll expand this section when you're ready to act on it.