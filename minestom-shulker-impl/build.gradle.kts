plugins {
    `java-library`
}

dependencies {
    api(project(":api"))
    api(project(":redis-provider"))
    implementation(variantOf(libs.shulker.server.agent) { classifier("minestom") })
}
