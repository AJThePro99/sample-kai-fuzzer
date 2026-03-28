package baseInterfaces

import dataModels.FuzzInput
import dataModels.SutResult
import dataModels.Verdict

/*
    The Orchestrator interface
 */
interface KaiFuzzer {
    suspend fun run(programs: Long, jobs: Int)
}

interface KaiInputGenerator {
    suspend fun generateInput() : FuzzInput
}

/*
    This interface is meant for SutHandlers that execute the FuzzInput() on multiple compiler versions for
    multiple backends
 */
interface KaiSutHandler {
    suspend fun executeOnCompilers(input: FuzzInput) : SutResult
}


interface KaiOracle {
    suspend fun evaluate(sutResult: SutResult) : Verdict
}

interface KaiIssueManager {
    suspend fun processVerdict(verdict: Verdict)
}

/*
    The reducer is supposed to take the offending code from the oracle, and perform reduction
    using the SUT Handler, and also the oracle again to check if the original input's Verdict, and the reduced
    input's verdict match.

    For now, the reducer is a stub, and once the workflows of the default Kai Fuzzer are complete,
    work on building an orchestrator with the reducer can begin. Till then, research on reduction techniques will be done.
 */
interface KaiReducer {
    suspend fun reduce(input: Verdict) : Verdict
}