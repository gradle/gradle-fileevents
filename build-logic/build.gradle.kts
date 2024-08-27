plugins {
    `kotlin-dsl`
}

group = "gradlebuild"

dependencies {
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("org.tukaani:xz:1.10")
}
