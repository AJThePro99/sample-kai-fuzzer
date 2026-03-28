package modules.oracle

import dataModels.SutResult
import dataModels.Verdict
import dataModels.VerdictStatus
import modules.utility.oracleUtility.ProcessRunnerUtility

/*
    This oracle compares the compiler outputs across all versions. And, only when they match, runs the compiled binaries
    and checks for runtime discrepancies.

    For checking only the compile time outputs, use CompileOnlyOracle()
 */
class RunnerOracle : CompileOnlyOracle() {
    override suspend fun evaluate(sutResult: SutResult): Verdict {
        // Do the compile only checks first
        val compileVerdict = super.evaluate(sutResult)

        // if compile phase has a bug, incorrect input, or unknown state, return immediately
        if (compileVerdict.status != VerdictStatus.CORRECT) {
            return compileVerdict
        }

        // At this point, all compilers outputs are found to be equal
        var foundRuntimeDiscrepancy = false
        val descriptionBuilder = StringBuilder()

        for ((backend, executionOutputs) in sutResult.outputList) {
            if (executionOutputs.isEmpty()) continue

            // Running all programs & mapping the compiler execution version to runtime output
            val runtimeResults = executionOutputs.map { output ->
                val jarPath = output.compiledFilePath ?: return Verdict(
                    sutResult,
                VerdictStatus.UNKNOWN,
                    "Missing jar path for version ${output.version}"
                )
                val command = listOf("java", "-cp", jarPath, "ProgramKt")
                val processResult = ProcessRunnerUtility.runProcess(command, timeoutMs = 5000)
                output.version to processResult
            }

            val (refVersion, refProcessResult) = runtimeResults.first()

            for (i in 1 until runtimeResults.size) {
                val (currentVersion, currentProcessResult) = runtimeResults[i]

                if (currentProcessResult.isTimeout != refProcessResult.isTimeout) {
                    foundRuntimeDiscrepancy = true
                    descriptionBuilder.appendLine("Backend $backend: Timeout mismatch! Version $refVersion timeout: ${refProcessResult.isTimeout}, version $currentVersion timeout=${currentProcessResult.isTimeout}")
                } else if (currentProcessResult.exitCode != refProcessResult.exitCode) {
                    foundRuntimeDiscrepancy = true
                    descriptionBuilder.appendLine("Backend $backend: Runtime Exit code mismatch! Version $refVersion returned ${refProcessResult.exitCode}, version $currentVersion returned ${currentProcessResult.exitCode}")
                } else if (currentProcessResult.stdout != refProcessResult.stdout) {
                    foundRuntimeDiscrepancy = true
                    descriptionBuilder.appendLine("Backend $backend: Runtime Output mismatch! stdout for $currentVersion differed from $refVersion")
                }
            }
        }
        return if (foundRuntimeDiscrepancy) {
            Verdict(sutResult, VerdictStatus.BUG_FOUND, descriptionBuilder.toString())
        } else {
            Verdict(sutResult, VerdictStatus.CORRECT, "All targeted compiler outputs and runtime outputs match")
        }
    }
}