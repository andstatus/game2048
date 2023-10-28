import korlibs.korge.gradle.korge

plugins {
    alias(libs.plugins.korge)
}

project.afterEvaluate {
//    tasks.map {
//        println("After evaluate Task: $it")
//    }
    tasks.findByName("preBuild")?.let {
        val taskToFix = "jvmProcessResources"
        println("Fixing dependency: $it depends on $taskToFix")
        it.dependsOn(taskToFix)
//        it.mustRunAfter(taskToFix)
    }
}

gradle.taskGraph.whenReady(closureOf<TaskExecutionGraph> {
    println("Found ${allTasks.size} tasks in task graph: $this")
    allTasks.forEach { task ->
        println(task)
        task.dependsOn.forEach { dep ->
            println("  - $dep")
        }
    }
})

korge {
    id = "org.andstatus.game2048"
    name = "2048 Open Fun Game"

    icon = file("src/commonMain/resources/res/drawable/app_icon.png")
    banner = file("src/commonMain/resources/res/drawable/app_icon.png")
    jvmMainClassName = "org.andstatus.game2048.MainKt"

    androidMinSdk = 24
    androidCompileSdk = 30
    androidTargetSdk = 31

    versionCode = 39
    version = "1.14.1"

    targetJvm()
    targetJs()
    targetAndroid()
}

//try {
//    tasks.named("lintVitalReportRelease").dependsOn("jvmProcessResources")
//} catch (e: Exception) {
//    // Ignored
//}
