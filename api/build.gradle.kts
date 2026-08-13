plugins {
    `java-library`
    `maven-publish`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    compileOnly(libs.configlang.api)
    // Adventure Component is used by IAuthPrompt; the library itself targets Java 8+.
    compileOnly(libs.adventure.api)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = "com.oxipro.idcraft"
            artifactId = "api"
            version = project.version.toString()
        }
    }
}