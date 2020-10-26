import com.soywiz.korge.gradle.KorgeGradlePlugin
import com.soywiz.korge.gradle.korge

buildscript {
    repositories {
        mavenLocal()
        maven { url = uri("https://dl.bintray.com/korlibs/korlibs") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
        mavenCentral()
        google()
    }
    dependencies {
        classpath("com.soywiz.korlibs.korge.plugins:korge-gradle-plugin:1.15.0.0")
    }
}

apply<KorgeGradlePlugin>()

korge {
    id = "org.andstatus.game2048"
    name = "2048"
    icon = file("src/commonMain/resources/game2048.png")
    jvmMainClassName = "org.andstatus.game2048.MainKt"

    androidMinSdk = 24
    androidCompileSdk = 29
    androidTargetSdk = 29
}
