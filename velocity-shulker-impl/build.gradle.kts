plugins {
    `java-library`
}

dependencies {
    api(project(":api"))
    compileOnly(libs.shulker.agent.velocity.api)
}
