pluginManagement {
    repositories {
        // ══════════════════════════════════════════════
        // 国内镜像源（优先级从高到低）
        // ══════════════════════════════════════════════
        maven { url = uri("https://maven.aliyun.com/repository/google") }       // 阿里云 Google 镜像
        maven { url = uri("https://maven.aliyun.com/repository/public") }       // 阿里云 Maven Central 镜像
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") } // 阿里云 Gradle Plugin 镜像

        // ══════════════════════════════════════════════
        // 官方源（兜底，国内慢但保底可用）
        // ══════════════════════════════════════════════
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ══════════════════════════════════════════════
        // 国内镜像源（优先级从高到低）
        // ══════════════════════════════════════════════
        maven { url = uri("https://maven.aliyun.com/repository/google") }   // 阿里云 Google 镜像
        maven { url = uri("https://maven.aliyun.com/repository/public") }   // 阿里云 Maven Central 镜像

        // ══════════════════════════════════════════════
        // 官方源（兜底）
        // ══════════════════════════════════════════════
        google()
        mavenCentral()
    }
}

rootProject.name = "FocusGuard"
include(":app")
