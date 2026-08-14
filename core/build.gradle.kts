plugins {
    `java-library`
}

dependencies {
    implementation(project(":db-postgres"))
    implementation(project(":db-mysql"))
    implementation(project(":redis-provider"))
    implementation(libs.configlang.api)
    implementation(libs.cssdb)
    api(project(":api"))
    api(libs.adventure.api)
    api(libs.adventure.minimessage)

    implementation(libs.password4j)
    implementation(libs.gson)

    compileOnly(libs.slf4j.api)
    compileOnly(libs.log4j.core)
}
