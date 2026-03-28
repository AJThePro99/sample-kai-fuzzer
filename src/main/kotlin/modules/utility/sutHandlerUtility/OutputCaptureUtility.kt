package modules.utility.sutHandlerUtility

import java.io.ByteArrayOutputStream
import java.io.PrintStream

data class CapturedOutput<T>(
    val result: T?,
    val output: String,
    val exception: Exception?
)

/*
    Utility module for capturing the outputs after compiler processing
 */
object OutputCaptureUtility {
    fun <T> capturePrintStream(action: (PrintStream) -> T): CapturedOutput<T> {
        val outputStream = ByteArrayOutputStream()
        val printStream = PrintStream(outputStream)

        var exception: Exception? = null
        var result : T? = null

        try {
            result = action(printStream)
        } catch (e : Exception) {
            exception = e;
            e.printStackTrace(printStream)
        } finally {
            printStream.flush()
        }

        return CapturedOutput(
            result = result,
            output = outputStream.toString(),
            exception = exception
        )
    }
}