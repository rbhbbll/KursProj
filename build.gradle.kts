plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
dependencies {
    implementation("org.postgresql:postgresql:42.5.1")
    implementation("org.mindrot:jbcrypt:0.4")
}
tasks.register<JavaExec>("run") {
    mainClass.set("org.example.Main")
    classpath = sourceSets.main.get().runtimeClasspath
}