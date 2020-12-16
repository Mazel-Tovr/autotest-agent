import org.junit.experimental.categories.Category
import spock.lang.Specification

@Category(UnitTest.class)
class MessageServiceSpec extends Specification {

    def 'Get message'() {
        URLConnection con = (URLConnection) new URL("http://postman-echo.com/headers").openConnection()
        def bytes = con.inputStream.bytes
        String expectedContent = URLEncoder.encode("Get message", "UTF-8")
        expect: 'Should return the correct message'
        println 'Should return the correct message'
        new String(bytes).contains(expectedContent)
    }

    def "numbers to the power of two"(int a, int b, int c) {
        URLConnection con = (URLConnection) new URL("http://postman-echo.com/headers").openConnection()
        def bytes = con.inputStream.bytes
        String expectedContent = URLEncoder.encode("numbers to the power of two", "UTF-8")
        expect:
        new String(bytes).contains(expectedContent)

        where:
        a | b | c
        1 | 2 | 1
        2 | 2 | 4
        3 | 2 | 9
    }
}
