package modules.inputGenerator

import baseInterfaces.KaiInputGenerator
import dataModels.FuzzInput

/*
    This input generator send only one Kotlin file, that the user passes.
    When using this generator, keep the program count in the orchestrator to 1.
    Otherwise, you'll be running the same program several times.

    This input generator is for specific testing purposes only.
 */
class SingleInputGenerator(
    private val sourceCode: String
) : KaiInputGenerator {
    override suspend fun generateInput(): FuzzInput {
        return FuzzInput(
            sourceCode = sourceCode,
            generatorId = "SingleInputGenerator"
        )
    }
}