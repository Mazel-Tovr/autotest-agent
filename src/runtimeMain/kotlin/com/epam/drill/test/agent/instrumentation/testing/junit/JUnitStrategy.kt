package com.epam.drill.test.agent.instrumentation.testing.junit

import com.epam.drill.test.agent.instrumentation.AbstractTestStrategy
import com.epam.drill.test.agent.TestListener
import com.epam.drill.test.agent.actions.TestResult
import javassist.*
import javassist.expr.*
import java.security.ProtectionDomain

@Suppress("unused")
object JUnitStrategy : AbstractTestStrategy() {
    private const val engineSegment = """[engine:junit]"""

    override val id: String = "junit"
    override fun permit(ctClass: CtClass): Boolean {
        return ctClass.name == "org.junit.runner.notification.RunNotifier" ||
                ctClass.name == "org.junit.internal.runners.statements.RunBefores" ||
                ctClass.name == "org.junit.internal.runners.statements.RunAfters"

    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {

        return when (ctClass.name) {
            "org.junit.runner.notification.RunNotifier" -> runNotifierInstrumentation(
                ctClass,
                pool,
                classLoader,
                protectionDomain
            )
            "org.junit.internal.runners.statements.RunBefores" -> runBeforesInstrumentation(
                ctClass,
                pool,
                classLoader,
                protectionDomain
            )
            "org.junit.internal.runners.statements.RunAfters" -> runAftersInstrumentation(
                ctClass,
                pool,
                classLoader,
                protectionDomain
            )

            else -> ctClass.toBytecode()
        }

    }

    private fun runNotifierInstrumentation(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {
        val cc: CtClass = pool.makeClass("MyList")
        cc.superclass = pool.get("org.junit.runner.notification.RunListener")
        cc.addField(CtField.make("org.junit.runner.notification.RunListener mainRunner = null;", cc))
        cc.addConstructor(
            CtNewConstructor.make(
                """
                    public MyList(org.junit.runner.notification.RunListener mainRunner) {
                        this.mainRunner = mainRunner;
                    }
                        """.trimMargin(), cc
            )
        )
        val dp = """description"""
        cc.addMethod(
            CtMethod.make(
                """
                    public void testRunStarted(org.junit.runner.Description $dp) throws Exception {
                        this.mainRunner.testRunStarted($dp);
                    }
                        """.trimIndent(),
                cc
            )
        )

        cc.addMethod(
            CtMethod.make(
                """
                    public void testStarted(org.junit.runner.Description $dp) throws Exception {
                        this.mainRunner.testStarted($dp);
                        ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}("$engineSegment/${classSegment(
                    dp
                )}/${methodSegment(dp)}");
                    }
                        """.trimIndent(),
                cc
            )
        )

        cc.addMethod(
            CtMethod.make(
                """
                public void testFinished(org.junit.runner.Description $dp) throws Exception {
                    this.mainRunner.testFinished(description);
                    ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}("$engineSegment/${classSegment(
                    dp
                )}/${methodSegment(dp)}", "${TestResult.PASSED.name}");
                }
                        """.trimIndent(),
                cc
            )
        )

        cc.addMethod(
            CtMethod.make(
                """
                public void testRunFinished(org.junit.runner.Result result) throws Exception {
                    this.mainRunner.testRunFinished(result);
                }
                        """.trimIndent(),
                cc
            )
        )

        val failureParamName = """failure"""
        val desct = """$failureParamName.getDescription()"""
        cc.addMethod(
            CtMethod.make(
                """
                public void testFailure(org.junit.runner.notification.Failure $failureParamName) throws Exception {
                    this.mainRunner.testFailure($failureParamName);
                    ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}("$engineSegment/${classSegment(
                    desct
                )}/${methodSegment(desct)}", "${TestResult.FAILED.name}");
                }
                        """.trimIndent(),
                cc
            )
        )

        cc.addMethod(
            CtMethod.make(
                """
                public void testAssumptionFailure(org.junit.runner.notification.Failure $failureParamName) {
                    this.mainRunner.testAssumptionFailure(failure);
                }
                        """.trimIndent(),
                cc
            )
        )

        cc.addMethod(
            CtMethod.make(
                """
                public void testIgnored(org.junit.runner.Description $dp) throws Exception {
                    this.mainRunner.testIgnored($dp);
                    ${TestListener::class.java.name}.INSTANCE.${TestListener::testIgnored.name}("$engineSegment/${classSegment(
                    dp
                )}/${methodSegment(dp)}");      
                }
                        """.trimIndent(),
                cc
            )
        )

        cc.toClass(classLoader, protectionDomain)
        ctClass.getDeclaredMethod("addListener").insertBefore(
            """
                $1= new MyList($1);
            """.trimIndent()
        )
        ctClass.getDeclaredMethod("addFirstListener").insertBefore(
            """
                $1= new MyList($1);
            """.trimIndent()
        )
        return ctClass.toBytecode()
    }

    private fun methodSegment(descriptionParamName: String) =
        """[method:"+$descriptionParamName.getMethodName()+"]"""

    private fun classSegment(descriptionParamName: String) = """[class:"+$descriptionParamName.getClassName()+"]"""

    //TODO it could be replace on annotationInstrumentation(ctClass: CtClass,
    //        pool: ClassPool,
    //        classLoader: ClassLoader?,
    //        protectionDomain: ProtectionDomain?
    //        codeLine: Int,
    //        variableName: String)

    private fun runBeforesInstrumentation(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {

        ctClass.getDeclaredMethod("evaluate").setBody(
            """
            {
                java.util.Iterator iterator = (java.util.Iterator) befores.iterator();
                while (iterator.hasNext()) {
                org.junit.runners.model.FrameworkMethod method = (org.junit.runners.model.FrameworkMethod) iterator.next();
                ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}
                                    ("$engineSegment/" + method.getDeclaringClass().getName() + "/" + method.getName());
                    method.invokeExplosively(target,new Object[0]);
                ${TestListener::class.java.name}.INSTANCE.${TestListener::testFinished.name}
                                    ("$engineSegment/" + method.getDeclaringClass().getName() + "/" + method.getName(),"${TestResult.PASSED.name}");
                }
                next.evaluate();

            }
            """.trimIndent()
        )
        return ctClass.toBytecode();
    }

    private fun runAftersInstrumentation(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {

        ctClass.getDeclaredMethod("evaluate").insertAt(
            33,
            """
            ${TestListener::class.java.name}.INSTANCE.${TestListener::testStarted.name}
                                ("$engineSegment/" + each.getDeclaringClass().getName() + "/" + each.getName());

        """.trimIndent()
        )
        return ctClass.toBytecode();
    }


}
