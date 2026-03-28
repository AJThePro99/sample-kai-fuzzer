package modules.oracle

import baseInterfaces.KaiOracle
import dataModels.SutResult
import dataModels.Verdict
import dataModels.VerdictStatus

/*
    This oracle compares only compiler outputs across different versions.
    It does not run the compiled programs, and is useful when wanting to only check the discrepancies during compilation

    For the proper two-fold checking process, use RunnerOracle()
 */

open class CompileOnlyOracle : KaiOracle {
    override suspend fun evaluate(sutResult: SutResult): Verdict {
        var foundCompileDiscrepancy = false
        var allCompileFailed = true
        var allCompilePassed = true

        val descriptionBuilder = StringBuilder()

        for ((backend, executionOutputs) in sutResult.outputList) {
            if (executionOutputs.isEmpty()) continue

            val referenceOutput = executionOutputs.first()

            for (output in executionOutputs) {
                if (output.exitCode != 0) {
                    allCompilePassed = false
                } else {
                    allCompileFailed = false
                }
            }

            // Check compile discrepancies
            for (i in 1 until executionOutputs.size) {
                val currentOutput = executionOutputs[i]
                if (currentOutput.exitCode != referenceOutput.exitCode) {
                    foundCompileDiscrepancy = true
                    descriptionBuilder.appendLine("Backend $backend: Compiler Exit code mismatch! Version ${referenceOutput.version} returned ${referenceOutput.exitCode}, but version ${currentOutput.version} returned ${currentOutput.exitCode}")
                }
            }
        }

        if (allCompileFailed) {
            return Verdict(
                sutResult,
                VerdictStatus.INVALID_INPUT,
                "All compilers failed to compile the input. Likely an invalid generated input."
            )
        }

        if (foundCompileDiscrepancy) {
            return Verdict(sutResult, VerdictStatus.BUG_FOUND, descriptionBuilder.toString())
        }

        if (!allCompilePassed) {
            return Verdict(
                sutResult,
                VerdictStatus.UNKNOWN,
                "Some compilers passed and some failed identically without triggering discrepancy? This should be unreachable or handled."
            )
        }

        return Verdict(sutResult, VerdictStatus.CORRECT, "All targeted compiler outputs match perfectly.")
    }
}