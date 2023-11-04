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
    androidCompileSdk = 33
    androidTargetSdk = 33

    versionCode = 41
    version = "1.14.3"

    androidManifestApplicationChunk(
        """
        <provider android:name="org.andstatus.game2048.data.FileProvider"
            android:authorities="org.andstatus.game2048.data.FileProvider"
            android:exported="true" />
		<activity android:name=".MyMainActivity"
			android:banner="@drawable/app_banner"
			android:icon="@drawable/app_icon"
			android:label="2048 Open Fun Game"
			android:logo="@drawable/app_icon"
			android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"
			android:screenOrientation="sensor"
			android:exported="true"
		>
			<intent-filter>
				<action android:name="android.intent.action.MAIN"/>
				<category android:name="android.intent.category.LAUNCHER"/>
			</intent-filter>
		</activity>
        <!-- comment out auto-generated activity   
        """.trimIndent()
    )
    androidManifestChunk(
        """
        -->    
	</application>
        """.trimIndent()
    )

    targetJvm()
    targetJs()
    targetAndroid()
}

//try {
//    tasks.named("lintVitalReportRelease").dependsOn("jvmProcessResources")
//} catch (e: Exception) {
//    // Ignored
//}
