package com.epam.drill.test.agent.instrumentation.http.ok

import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.instrumentation.*
import com.epam.drill.test.agent.instrumentation.http.Log
import javassist.*
import java.security.ProtectionDomain

class OkHttpClient : Strategy() {

    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.interfaces.any { "drill." + it.name == "okhttp3.internal.http.HttpCodec" }
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray {
        val sendRequestHeader = kotlin.runCatching {
            ctClass.getDeclaredMethod("writeRequestHeaders")
        }.onFailure {
            logger.error(it) { "Error while instrumenting the class ${ctClass.name}" }
        }
        sendRequestHeader.getOrNull()?.insertBefore(
            """
                if ($IF_CONDITION) {
                ${Log::class.java.name}.INSTANCE.${Log::injectHeaderLog.name}($TEST_NAME_VALUE_CALC_LINE,$SESSION_ID_VALUE_CALC_LINE);
                    $1 = $1.newBuilder()
                            .addHeader($TEST_NAME_CALC_LINE)
                            .addHeader($SESSION_ID_CALC_LINE)
                            .build();
                }
            """.trimIndent()
        )
        return ctClass.toBytecode()
    }
}
