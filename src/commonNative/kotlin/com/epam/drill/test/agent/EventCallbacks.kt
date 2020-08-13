@file:Suppress("UNUSED_PARAMETER", "UNUSED")

package com.epam.drill.test.agent

import com.epam.drill.test.agent.actions.*
import com.epam.drill.test.agent.instrumenting.*
import com.epam.drill.hook.io.tcp.injectedHeaders
import com.epam.drill.interceptor.*
import com.epam.drill.jvmapi.JNIEnvPointer
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.jvmapi.vmGlobal
import com.epam.drill.test.agent.penetration.*
import kotlinx.cinterop.*
import kotlin.native.concurrent.*

fun enableJvmtiEventVmDeath(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, thread)
}

fun enableJvmtiEventVmInit(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, thread)
}

fun enableJvmtiEventClassFileLoadHook(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, thread)
}

@Suppress("UNUSED_PARAMETER")
fun vmDeathEvent(jvmtiEnv: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?) {
    mainLogger.debug { "vmDeathEvent" }
}

fun callbackRegister() = memScoped {
    val eventCallbacks = alloc<jvmtiEventCallbacks>()
    eventCallbacks.VMInit = staticCFunction(::jvmtiEventVMInitEvent)
    eventCallbacks.VMDeath = staticCFunction(::vmDeathEvent)
    eventCallbacks.ClassFileLoadHook = staticCFunction(::classFileLoadHookEvent)
    SetEventCallbacks(eventCallbacks.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
    enableJvmtiEventVmInit()
    enableJvmtiEventVmDeath()
}

fun jvmtiEventVMInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) {
    mainLogger.debug { "Init event" }
    initRuntimeIfNeeded()
    StrategyManager.initialize(SessionController.agentConfig.value.rawFrameworkPlugins)
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, null)
    configureHooks()
}

fun configureHooks() {
    configureHttpInterceptor()
    mainLogger.debug { "Interceptor configured" }
    injectedHeaders.value = {
        mainLogger.debug { "Injecting headers" }
        val lastTestName = SessionController.testName.value
        val sessionId = SessionController.sessionId.value
        mainLogger.debug { "Adding headers: $lastTestName to $sessionId" }
        mapOf(
            "drill-test-name" to lastTestName,
            "drill-session-id" to sessionId
        )
    }.freeze()
}