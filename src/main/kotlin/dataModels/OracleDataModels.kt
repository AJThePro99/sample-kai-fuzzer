package dataModels

enum class VerdictStatus {
    CORRECT,        // All compilations succeed and match
    BUG_FOUND,      // When compilation results across versions do not match
    INVALID_INPUT,  // All compilers fail with the same error (Bug in input Generator)
    UNKNOWN         //
}

data class Verdict(
    val result: SutResult,
    val status: VerdictStatus,
    val description: String? = null
)