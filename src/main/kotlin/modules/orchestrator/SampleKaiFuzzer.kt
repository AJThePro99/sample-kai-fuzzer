package modules.orchestrator

import baseInterfaces.KaiFuzzer
import baseInterfaces.KaiInputGenerator
import baseInterfaces.KaiIssueManager
import baseInterfaces.KaiOracle
import baseInterfaces.KaiSutHandler
import dataModels.FuzzInput
import dataModels.SutResult
import dataModels.Verdict
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope


/*
    This Orchestrator works without a reducer module.
    It generates the input and passes it to the SUT Handler
    The SUT Handler compiles the programs for all specified versions, and bundles it and passes it to Oracle
    The Oracle compares the outputs, for inconsistencies and delivers a verdict, and it's passed to the Issue Manager
    The Issue Manager neatly bundles it, and stores the results into the file system.
 */
class SampleKaiFuzzer(
    private val inputGenerator: KaiInputGenerator,
    private val sutHandler: KaiSutHandler,
    private val oracle: KaiOracle,
    private val issueManager: KaiIssueManager
) : KaiFuzzer {
    /*
        orchestrator directs inputGenerator to generate 'program' number of Kotlin programs
        and run on 'jobs' number of jobs.
     */
    override suspend fun run(programs: Long, jobs: Int): Unit = supervisorScope {
        /*
            Kotlin channels for maintaining throughput, and avoid OOM errors
            and runtime errors when memory becomes scarce
            ideally, capacity is not hardcoded, but this is for the sake of an example
         */
        val inputChannel = Channel<FuzzInput>(capacity = 64)
        val resultChannel = Channel<SutResult>(capacity = 64)


        // The input generator coroutine
        launch {
            for(i in 1..programs) {
                val input : FuzzInput = inputGenerator.generateInput()
                inputChannel.send(input)
            }
            inputChannel.close()
            // closing inputChannel once all the programs are generated
        }

        // The SUT Handler coroutine
        val sutWorkers = (1..jobs).map {
            launch {
                for (input in inputChannel) {
                    val result : SutResult = sutHandler.executeOnCompilers(input)
                    resultChannel.send(result)
                }
            }
        }

        // join all sutWorkers coroutines and close result channel since all programs are compiled
        // and verdicts have been delivered
        launch {
            sutWorkers.joinAll()
            resultChannel.close()
        }

        // The Oracle coroutine
        // Delivers the verdict for each SutResult, and passes it to the issue manager for storage
        // Issue manager only stores outputs deemed to be incorrect by the oracle
        launch {
            for (result in resultChannel) {
                val verdict : Verdict = oracle.evaluate(result)
                issueManager.processVerdict(verdict)
            }
        }
    }
}