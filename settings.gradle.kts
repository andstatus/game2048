pluginManagement {
    repositories {
        mavenLocal(); mavenCentral(); google(); gradlePluginPortal()
    }
}

buildscript {
    val libsTomlFile = File(this.sourceFile?.parentFile, "gradle/libs.versions.toml").readText()
    var plugins = false
    var version = ""
    for (line in libsTomlFile.lines().map { it.trim() }) {
        if (line.startsWith("#")) continue
        if (line.startsWith("[plugins]")) plugins = true
        if (plugins && line.startsWith("korge") && Regex("^korge\\s*=.*").containsMatchIn(line)) version = Regex("version\\s*=\\s*\"(.*?)\"").find(line)?.groupValues?.get(1) ?: error("Can't find korge version")
    }
    if (version.isEmpty()) error("Can't find korge version in $libsTomlFile")

    repositories { mavenLocal(); mavenCentral(); google(); gradlePluginPortal() }

    dependencies {
        classpath("com.soywiz.korge.settings:com.soywiz.korge.settings.gradle.plugin:$version")
    }
}

apply(plugin = "com.soywiz.korge.settings")
