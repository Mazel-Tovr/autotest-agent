package com.epam.drill.auto.test.agent.penetration

import javassist.CtClass

abstract class Strategy {

    abstract fun permit(ctClass: CtClass): Boolean

    abstract fun instrument(ctClass: CtClass): ByteArray?
}