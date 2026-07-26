plugins {
    `java-library`
}

dependencies {
    api(project(":api"))
    implementation("io.shulkermc:shulker-server-agent:0.13.0-SNAPSHOT:minestom")
}
