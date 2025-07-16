# Maven Local Publish
We can publish the module to local Maven repository (in .m2/repository),
That way, we can test it just like a real dependency. before upload it to live central (i.e JitPack)

## In Module/build.gradle

```groovy
plugins{
    // other plugins
    id 'maven-publish' // <--- Add this plugin
}

android{
    // other scripts
    
    // Optionally specify variant
    publishing {
        singleVariant('release') {
            // we'll publish the 'release' variant
            // We can specify 'release' or other variants here
            withSourcesJar()
            withJavadocJar()
        }
    }
}

// --- Maven Local Publish Script
afterEvaluate {
    publishing {
        publications {
            release(MavenPublication){
                // module info
                groupId = 'com.domain.myLib'   // use a preferred namespace
                artifactId = 'myLib'
                version = '1.0.0-local'                   // we can change this per test

                // identify which component to publish, its usually `components.release`
                // or we can specify our custom flavor release, like `components.dev`
                from components.release
            }
        }

        repositories {
            mavenLocal() // <--- 6. Predefined local Maven repository
        }
    }
}
```

- We can add dependency POM info inside the release{} block
```groovy
//...
release(MavenPublication){
    // ...
     pom {
         name = 'Your Android Utility Library'
         description = 'A set of utility functions for Android projects.'
         url = 'https://github.com/your-github-username/your-repo-name'
         licenses {
             license {
                 name = 'The Apache License, Version 2.0'
                 url = 'http://www.apache.org/licenses/LICENSE-2.0.txt'
             }
         }
         developers {
             developer {
                 id = 'yourid'
                 name = 'Your Name'
                 email = 'your.email@example.com'
             }
         }
         scm {
             connection = 'scm:git:github.com/your-github-username/your-repo-name.git'
             developerConnection = 'scm:git:ssh://github.com/your-github-username/your-repo-name.git'
             url = 'https://github.com/your-github-username/your-repo-name'
         }
     }
}
```

## Publish To Local Maven
[1] Using Terminal
- Navigate to the  project's root directory in your terminal and run:
```bash
./gradlew :myLib:publishToMavenLocal
```
- This will create:
  <project-root>/build/local-maven/com/yourdomain/myLib/myLib/1.0.0-local/...

[2] Using Gradle Tool Window
1. Open the Gradle tool window (usually on the right side of Android Studio).
2. Expand your_project_name > myLib > Tasks > publishing.
3. Double-click publishToMavenLocal.
After running this task, an AAR file, POM file, sources JAR, and Javadoc JAR 
will be placed in the local Maven repository.

## Usage
Now we can use our library in other projects like this:
- (1) In settings.gradle, Add mavenLocal() to your repositories block:
```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ...
        mavenLocal() // <--- Add this line
    }
} 
```
- (2) In build.gradle (app-level), add the dependency:
```groovy
dependencies {
    // ... other dependencies
    implementation 'com.domain.myLib:1.0.0-local' // <--- Your local dependency
} 
```

