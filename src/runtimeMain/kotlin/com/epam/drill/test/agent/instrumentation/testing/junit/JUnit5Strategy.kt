package com.epam.drill.test.agent.instrumentation.testing.junit

import com.epam.drill.test.agent.instrumentation.AbstractTestStrategy
import com.epam.drill.test.agent.TestListener
import com.epam.drill.test.agent.actions.*
import javassist.*
import javassist.expr.*
import java.lang.reflect.*
import java.security.ProtectionDomain

@Suppress("unused")
object JUnit5Strategy : AbstractTestStrategy() {
    override val id: String = "junit"
    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == "org.junit.platform.engine.support.hierarchical.NodeTestTaskContext" ||
                ctClass.name == "org.junit.jupiter.engine.execution.ExecutableInvoker"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {
        return when (ctClass.name) {
            "org.junit.platform.engine.support.hierarchical.NodeTestTaskContext" -> nodeTestTaskContextInstrumentation(
                ctClass,
                pool,
                classLoader,
                protectionDomain
            )
            "org.junit.jupiter.engine.execution.ExecutableInvoker" -> executableInvokerInstrumentation(
                ctClass,
                pool,
                classLoader,
                protectionDomain
            )
            else -> ctClass.toBytecode()
        }
    }


    private fun nodeTestTaskContextInstrumentation(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {
        val cc: CtClass = pool.makeClass("MyList")
        cc.interfaces = arrayOf(pool.get("org.junit.platform.engine.EngineExecutionListener"))
        cc.addField(CtField.make("org.junit.platform.engine.EngineExecutionListener mainRunner = null;", cc))
        cc.addConstructor(
            CtNewConstructor.make(
                """
                            public MyList(org.junit.platform.engine.EngineExecutionListener mainRunner) { 
                               this.mainRunner = mainRunner;
                            }
                        """.trimMargin()
                , cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                            public void dynamicTestRegistered(org.junit.platform.engine.TestDescriptor testDescriptor) {
                                mainRunner.dynamicTestRegistered(testDescriptor);
                            }
                        """.trimIndent(),
                cc
            )
        )
        val testUniqueId = "testDescriptor.getUniqueId().toString()"
        cc.addMethod(
            CtMethod.make(
                """
                            public void executionSkipped(org.junit.platform.engine.TestDescriptor testDescriptor, String reason) {
                                mainRunner.executionSkipped(testDescriptor, reason);
                                if (!testDescriptor.isContainer()) {
                                    ${TestListener::class.java.name}.INSTANCE.${TestListener::testIgnored.name}($testUniqueId);
                                }
                            }
                        """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                            public void executionStarted(org.junit.platform.engine.TestDescriptor testDescriptor) {
                                mainRunner.executionStarted(testDescriptor);
                                if (!testDescriptor.isContainer()) {
                                    ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}($testUniqueId);
                                }
                            }
                        """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                            public void executionFinished(org.junit.platform.engine.TestDescriptor testDescriptor, org.junit.platform.engine.TestExecutionResult testExecutionResult) {
                                mainRunner.executionFinished(testDescriptor, testExecutionResult);
                                if (!testDescriptor.isContainer()) {
                                    ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}($testUniqueId, testExecutionResult.getStatus().name());
                                }
                            }
                        """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                            public void reportingEntryPublished(org.junit.platform.engine.TestDescriptor testDescriptor, org.junit.platform.engine.reporting.ReportEntry entry) {
                                mainRunner.reportingEntryPublished(testDescriptor, entry);
                            }
                        """.trimIndent(),
                cc
            )
        )
        cc.toClass(classLoader, protectionDomain)

        ctClass.constructors.first().insertBefore(
            """
                    $1 = new MyList($1);
            """.trimIndent()
        )
        return ctClass.toBytecode()
    }

    private fun executableInvokerInstrumentation(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {

        val annotation = "${'$'}1.getAnnotation(org.junit.jupiter.api.BeforeAll.class) != null"
//                "${'$'}1.getAnnotation(org.junit.jupiter.api.AfterAll.class) != null || " +
//                "${'$'}1.getAnnotation(org.junit.jupiter.api.BeforeEach.class) != null ||" +
//                "${'$'}1.getAnnotation(org.junit.jupiter.api.AfterEach.class) != null"

        ctClass.declaredMethods[1].instrument(object : ExprEditor() {
            override fun edit(m: MethodCall) {
                m.replace(
                    """
                if($annotation) { 
                    ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}(${'$'}1.getName());
                    ${'$'}_ = ${'$'}proceed(${'$'}${'$'});
                     ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}(${'$'}1.getName(),${TestResult.PASSED.name});
                } else {
                 ${'$'}_ = ${'$'}proceed(${'$'}${'$'});
                }
                
                """.trimIndent()
                )
            }
        })

        return ctClass.toBytecode()
    }
}
