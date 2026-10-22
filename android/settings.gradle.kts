import java.util.Properties

// 优先从 local.properties 读取 JDK 路径（本机配置，不提交 Git）
val localPropertiesFile = file("local.properties")
if (localPropertiesFile.exists()) {
    val localProperties = Properties()
    localPropertiesFile.inputStream().use { localProperties.load(it) }
    val javaHome = localProperties.getProperty("org.gradle.java.home")
    if (!javaHome.isNullOrBlank()) {
        System.setProperty("org.gradle.java.home", javaHome.trim())
    }
}

pluginManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // com.amap.api 在 Maven Central，须优先解析，否则 IDE 可能报「无法解析 amap」
        mavenCentral()
        google()
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "gps"
include(":app")
 