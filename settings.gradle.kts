rootProject.name = "SoulSpeedHarness"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "papermc-repo"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven {
            name = "rikonardo"
            url = uri("https://maven.rikonardo.com/releases")
        }
    }
}
