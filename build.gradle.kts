plugins {
    kotlin("jvm") version "1.3.72"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    `maven-publish`
}

group = properties["pluginGroup"]!!
version = properties["pluginVersion"]!!

repositories {
    mavenCentral()
    maven(url = "https://papermc.io/repo/repository/maven-public/") //paper
    maven(url = "https://repo.dmulloy2.net/nexus/repository/public/") //protocollib
    maven(url = "https://jitpack.io/") //tap, psychic
    maven(url = "https://maven.enginehub.org/repo/") //worldedit
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8")) //kotlin
    compileOnly("junit:junit:4.12") //junit
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT") //paper
    implementation("com.github.monun:tap:3.3.3") //tap
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.2") //worldedit
    implementation("com.github.monun:kommand:0.7.+")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
    javadoc {
        options.encoding = "UTF-8"
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    processResources {
        filesMatching("**/*.yml") {
            expand(project.properties)
        }
    }
    shadowJar {
        archiveClassifier.set("dist")
    }
    create<Copy>("distJar") {
        from(shadowJar)
        into("C:\\Users\\User00\\Desktop\\server\\plugins")
    }
    create<Copy>("copyToServer") {
        from(shadowJar)
        var dest = File(rootDir, ".server/plugins")
        if(File(dest, shadowJar.get().archiveFileName.get()).exists()) dest = File(dest, "update")
        into(dest)
    }
    create<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }
}
publishing {
    publications {
        create<MavenPublication>("Rocket") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
        }
    }
}

if (!hasProperty("debug")) {
    tasks {
        shadowJar {
            relocate("com.github.noonmaru.kommand", "com.github.devil0414.rocket.shaded")
        }
    }
}