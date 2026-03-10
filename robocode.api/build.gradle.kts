plugins {
    id("net.sf.robocode.java-conventions")
    java
    signing
    `maven-publish`
}

java {
    withJavadocJar()
    withSourcesJar()
}

description = "Robocode API"

tasks {
    javadoc {
        source = sourceSets["main"].java
        exclude("robocode/exception/**")
        exclude("robocode/robocodeGL/**")
        exclude("gl4java/**")
        exclude("net/sf/robocode/**")

        options.windowTitle = "Robocode ${project.version} API"
        (options as StandardJavadocDocletOptions).apply {
            docTitle = "<h1>Robocode ${project.version} API</h1>"
            docFilesSubDirs(true)
            use(false)
            author(true)
            isNoTimestamp = false
            bottom("Copyright &#169; 2001-2025 <a href=\"https://robocode.sourceforge.io\">Robocode</a>. All Rights Reserved.")
            // Oracle Java 8 docs no longer available; omit links to avoid fetch errors
            charSet("UTF-8")
            source("1.8")
            excludeDocFilesSubDir("robocode.exception", "net.sf.robocode", "gl4java", "robocode.robocodeGL")
                .addStringsOption("exclude", ":").value =
                listOf("gl4java", "robocode.exception", "net.sf.robocode", "robocode.robocodeGL")
        }
    }

    jar {
        dependsOn(javadoc)

        manifest {
            attributes(mapOf("Main-Class" to "robocode.Robocode"))
        }
        archiveFileName.set("robocode.jar")
        into("") {
            from("../versions.md")
        }
    }
}