package modules.utility.oracleUtility

import java.io.File
import java.util.concurrent.TimeUnit

data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val isTimeout: Boolean
)

object ProcessRunnerUtility {
    fun runProcess(command: List<String>, timeoutMs: Long = 5000, workingDir: File? = null) : ProcessResult {
        val processBuilder = ProcessBuilder(command)
        if (workingDir != null) {
            processBuilder.directory(workingDir)
        }
        processBuilder.redirectErrorStream(true)

        var isTimeout = false
        var exitCode = -1

        try {
            val process = processBuilder.start()

            val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            // if not finished in 5000ms, we assume it's an infinite loop, and instead of wasting time, we terminate it
            if (!finished) {
                isTimeout = true
                process.destroyForcibly()
                process.waitFor(100, TimeUnit.MILLISECONDS)
            } else {
                exitCode = process.exitValue()
            }

            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = ""

            return ProcessResult(
                exitCode = exitCode,
                stdout = stdout,
                stderr = stderr,
                isTimeout = isTimeout
            )
        } catch (e : Exception) {
            return ProcessResult(
                exitCode = -1,
                stdout = "",
                stderr = e.message ?: "Unknown error occurred while running process",
                isTimeout = false
            )
        }
    }
}