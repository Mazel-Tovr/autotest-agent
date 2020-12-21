import kotlinx.coroutines.*

plugins {
    kotlin("jvm")
//    id("com.epam.drill.agent.runner.autotest") apply false
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}



val register = tasks.register("startServer", CustomTask::class) {
    print("zz")
    dependsOn("jar")
    args = listOf(project.extra["adminHost"].toString(), project.extra["adminPort"].toString())
    classpath = files("build/libs/${project.name}.jar")
}


tasks.register("stopServer") {
    doLast {
        println(register.get().allJvmArgs)
        println("I should stop server but i'm not implemented what a disaster, isn't it ?")
        exec {
            this.commandLine("taskkill", "/f", "/im", "java.exe")
        }
    }

}



open class CustomTask : JavaExec() {
    override fun exec() {
        GlobalScope.launch {
            super.exec()
        }
    }
}

tasks.withType(Jar::class) {
    manifest {
        attributes["Manifest-Version"] = "1.0"
        attributes["Main-Class"] = "Main"
    }
}


