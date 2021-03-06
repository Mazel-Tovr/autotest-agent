import pack.HttpHeadersTest
import kotlin.test.Ignore
import kotlin.test.Test

class Test {

    @Test
    @Ignore
    fun simpleTestMethodName() {
        HttpHeadersTest.test(::simpleTestMethodName.name)
    }

    @Test
    fun `method with backtick names`() {
        HttpHeadersTest.test(::`method with backtick names`.name)
    }
    @Test
    fun `Кириллик леттерс`() {
        HttpHeadersTest.test(::`Кириллик леттерс`.name)
    }

    @Suppress("RemoveRedundantBackticks")
    @Test
    fun `shortBacktick`() {
        HttpHeadersTest.test(::`shortBacktick`.name)
    }
}
