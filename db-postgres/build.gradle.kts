plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    api(project(":api"))
    implementation(libs.hikaricp)
    implementation(libs.postgresql.driver)
}
