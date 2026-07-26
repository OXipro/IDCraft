plugins {
    application
    java
    `maven-publish`
    id("com.gradleup.shadow") version "9.6.1"
}

dependencies {
    implementation(project(":api"))
    implementation("net.minestom:minestom:2026.07.22-26.2")
}

application {
    mainClass = "com.oxipro.idcraft.minestom.IDCraftMinestomServer"
}

tasks.shadowJar {
    archiveBaseName.set("idcraft-minestom-server")
    archiveClassifier.set("")
    archiveVersion.set("")
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