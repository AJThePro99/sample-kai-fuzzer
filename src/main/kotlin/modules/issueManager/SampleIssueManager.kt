package modules.issueManager

import baseInterfaces.KaiIssueManager
import dataModels.Verdict
import dataModels.VerdictStatus
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Date

/*
    This issue manager will take the verdict, discard verdicts that are correct.
    And only store the ones that have been flagged by the oracle.

    For this sample implementation, this creates /outputs directory in the project root,
    and it will delete the correct .jar files from the temp file.

    This should not be in a production environment, but this is just for painless demonstration
 */

class SampleIssueManager(private val projectRoot: String = System.getProperty("user.dir")) : KaiIssueManager {
    override suspend fun processVerdict(verdict: Verdict) {
        when (verdict.status) {
            VerdictStatus.CORRECT -> handleCorrect(verdict)
            VerdictStatus.BUG_FOUND, VerdictStatus.INVALID_INPUT, VerdictStatus.UNKNOWN -> handleBugFound(verdict)
        }
    }

    // Delete all pre-generated output.jar files and their parent temp directories
    private fun handleCorrect(verdict: Verdict) {
        for ((_, outputs) in verdict.result.outputList) {
            for (output in outputs) {
                output.compiledFilePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        val parent = file.parentFile
                        file.delete()
                        parent?.deleteRecursively()
                    }
                }
            }
        }
    }

    private fun handleBugFound(verdict: Verdict) {
        val outputsDir = File(projectRoot, "outputs")
        if (!outputsDir.exists()) {
            outputsDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyy_MM_dd_HH-mm-ss_SSS").format(Date())
        val issueId = "${verdict.status.name}_${timestamp}"
        val issueDir = File(outputsDir, issueId)
        issueDir.mkdirs()

        // Saving the source code
        val sourceFile = File(issueDir, "Program.kt")
        sourceFile.writeText(verdict.result.input.sourceCode)

        val summaryBuilder = StringBuilder()
        summaryBuilder.appendLine("Status: ${verdict.status.name}")
        summaryBuilder.appendLine("Description: ${verdict.description}")
        summaryBuilder.appendLine("---")

        for ((backend, outputs) in verdict.result.outputList) {
            summaryBuilder.appendLine("Backend: $backend")
            for (output in outputs) {
                summaryBuilder.appendLine("    Version: ${output.version}")
                summaryBuilder.appendLine("    Compiler exit code: ${output.exitCode}")

                if (output.stderr.isNotBlank()) {
                    summaryBuilder.appendLine("    Compiler stderr: \n${output.stderr.prependIndent("    ")}")
                }

                output.compiledFilePath?.let { path ->
                    val jarFile = File(path)
                    if (jarFile.exists()) {
                        val destinationJar = File(issueDir, "output_${output.version}.jar")
                        Files.copy(jarFile.toPath(), destinationJar.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        summaryBuilder.appendLine("    Compiled Jar: ${destinationJar.name}")

                        val parent = jarFile.parentFile
                        jarFile.delete()
                        parent?.deleteRecursively()
                    }
                }
                summaryBuilder.appendLine()
            }
        }
        val summaryFile = File(issueDir, "summary.txt")
        return summaryFile.writeText(summaryBuilder.toString())
    }
}