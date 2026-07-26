plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api("com.oxipro.cmu.configlang:1.0:api")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = "com.oxipro.idcraft"
            artifactId = "api"
            version = project.version.toString()
        }
    }
}