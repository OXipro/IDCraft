plugins {
    java
}

allprojects {
    group = "com.oxipro.idcraft"
    version = "1.0.0-beta"

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }
}