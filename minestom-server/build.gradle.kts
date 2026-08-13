plugins {
    application
    java
    `maven-publish`
    id("com.gradleup.shadow") version "9.6.1"
}

dependencies {
    implementation(project(":api"))
    implementation(project(":core"))
    implementation(project(":db-postgres"))
    implementation(project(":db-mysql"))
    implementation(project(":redis-provider"))
    implementation(project(":minestom-shulker-impl"))
    implementation(libs.cssdb)
    implementation(libs.configlang.minestom)
    implementation(libs.minestom)

    implementation(libs.log4j.core)
    implementation(libs.log4j.slf4j2.impl)
}

application {
    mainClass = "com.oxipro.idcraft.minestom.IDCraftMinestomServer"
}

tasks.shadowJar {
    archiveBaseName.set("idcraft-minestom-server")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            from(components["shadow"])

            groupId = "com.oxipro.idcraft"
            artifactId = "minestom-server"
            version = project.version.toString()
        }
    }
}