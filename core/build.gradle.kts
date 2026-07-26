plugins {
    `java-library`
}

dependencies {
    api(project(":api"))
    implementation("io.shulkermc:shulker-server-agent")
    api("com.oxipro.cmu.configlang:1.0:minestom")
}
