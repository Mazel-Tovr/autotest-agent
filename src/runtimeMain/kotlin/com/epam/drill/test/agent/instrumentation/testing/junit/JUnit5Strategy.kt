package com.epam.drill.test.agent.instrumentation.testing.junit

import com.epam.drill.test.agent.instrumentation.AbstractTestStrategy
import com.epam.drill.test.agent.TestListener
import javassist.*
import java.security.ProtectionDomain

@Suppress("unused")
object JUnit5Strategy : AbstractTestStrategy() {
    override val id: String = "junit"
    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == "org.junit.platform.engine.support.hierarchical.NodeTestTaskContext"
    }

    override fun instrument(
        ctClass: CtClass,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {
        val pool = ClassPool.getDefault()
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
        cc.addMethod(
            CtMethod.make(
                """
                            public void executionSkipped(org.junit.platform.engine.TestDescriptor testDescriptor, String reason) {
                                mainRunner.executionSkipped(testDescriptor, reason);
                                ${TestListener::class.java.name}.INSTANCE.${TestListener::testIgnored.name}(testDescriptor.getUniqueId().toString());
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
                                ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}(testDescriptor.getUniqueId().toString());
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
                                ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}(testDescriptor.getUniqueId().toString(), testExecutionResult.getStatus().name());
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
}
