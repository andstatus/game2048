import com.soywiz.korge.gradle.KorgeGradlePlugin
import com.soywiz.korge.gradle.korge

buildscript {
    val korgePluginVersion: String by project

    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
    dependencies {
        classpath("com.soywiz.korlibs.korge.plugins:korge-gradle-plugin:$korgePluginVersion")
    }
}

apply<KorgeGradlePlugin>()

korge {
    id = "org.andstatus.game2048"
    name = "2048 Open Fun Game"

    icon = file("src/commonMain/resources/res/drawable/app_icon.png")
    banner = file("src/commonMain/resources/res/drawable/app_icon.png")
    jvmMainClassName = "org.andstatus.game2048.MainKt"

    androidMinSdk = 24
    androidCompileSdk = 29
    androidTargetSdk = 29

    targetJvm()
    targetJs()
    targetDesktop()

    // We need to switch to the "indirect" target only to generate build.gradle
    // and manually sync it with our customized game2048-android/build.gradle
    //targetAndroidIndirect()
    // The below stopped working after upgrade to Kotlin 1.5.0, etc.
    //targetAndroidDirect()
}
