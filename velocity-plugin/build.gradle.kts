plugins {
    application
    java
    `maven-publish`
    id("com.gradleup.shadow") version "9.6.1"
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    implementation(project(":api"))
    implementation(project(":core"))
    implementation(project(":db-postgres"))
    implementation(project(":db-mysql"))
    implementation(project(":redis-provider"))
    implementation(project(":velocity-shulker-impl"))
    implementation(libs.configlang.standalone)
    implementation(libs.configlang.velocity)
    implementation(libs.cssdb)
    implementation(libs.bstats.veloicty)
    compileOnly(libs.velocity)
    compileOnly(libs.floodgate)
    compileOnly(libs.slf4j.api)
    compileOnly(libs.log4j.core)
}

application {
    mainClass = "com.oxipro.idcraft.plugin.velocity.IDCraftVelocityPlugin"
}

tasks.shadowJar {
    archiveBaseName.set("idcraft-velocity-plugin")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
    manifest {
        attributes("Implementation-Title" to "IDCraft", "Implementation-Version" to project.version.toString())
    }

    // Relocate bStats into the plugin's package to avoid conflicts with other
    // plugins using bStats
    relocate("org.bstats", project.group.toString())
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            from(components["shadow"])

            groupId = "com.oxipro.idcraft.plugin"
            artifactId = "velocity-server"
            version = project.version.toString()
        }
    }
}