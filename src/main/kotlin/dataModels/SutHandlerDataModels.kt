package dataModels

import java.util.UUID

enum class SutBackend {
    JVM,
    NATIVE,
    JS,
    WASM
}

/*
    The SutHandler returns one SutResult for each FuzzInput() data object.
    Contains the bundled outputs for each compiler version and each backend.
 */
data class SutResult(
    val input: FuzzInput,
    // SutResult will map each backend with a list of outputs for each compiler version
    val outputList: Map<SutBackend, List<CompilerExecutionOutput>>
)

/*
    CompilerExecutionOutput() will contain the results for each version of the kotlin compiler
*/
data class CompilerExecutionOutput(
    val version: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val compiledFilePath: String? = null
)