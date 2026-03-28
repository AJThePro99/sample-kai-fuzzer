package modules.sutHandler

import baseInterfaces.KaiSutHandler
import dataModels.CompilerExecutionOutput
import dataModels.FuzzInput
import dataModels.SutBackend
import dataModels.SutResult
import modules.utility.sutHandlerUtility.KotlinCompilerDownloaderJVM
import java.io.File
import java.nio.file.Files

/*
    This sample SutHandler can only compile for the JVM backend
    Can be extended to handle compilations for other backends
 */
class SampleSutHandler(
    private val jvmVersions: List<String>
) : KaiSutHandler {
    private val downloader = KotlinCompilerDownloaderJVM()

    // checks, and downloads the compiler versions and stdlibs
    // this function is responsible for compiling the FuzzInput() on all compiler versions, one at a time.

    // the orchestrator will launch multiple executeOnCompilers(), according to the number of jobs. So,
    // it should be properly implemented
    override suspend fun executeOnCompilers(input: FuzzInput): SutResult {
        val compilerExecutions = mutableListOf<CompilerExecutionOutput>()
        for (version in jvmVersions) {
            // get the compiler for the current version
            val (compilerJar, trove4jJar, homeDir) = downloader.getCompilers(version)
            // compile to get the output
            val output = compileWithCompiler(
                sourceCode = input.sourceCode,
                version = version,
                compilerJar = compilerJar,
                trove4jJar = trove4jJar,
                homeDir = homeDir
            )
            // add the output to the list
            compilerExecutions.add(output)
        }

        return SutResult(
            input = input,
            outputList = mapOf(SutBackend.JVM to compilerExecutions)
        )
    }

    private fun compileWithCompiler(
        sourceCode: String,
        version: String,
        compilerJar: File,
        trove4jJar: File,
        homeDir: File
    ): CompilerExecutionOutput {
        val tempDir = Files.createTempDirectory("kotlin-$version-").toFile()
        val sourceFile = File(tempDir, "Program.kt").apply { writeText(sourceCode) }

        val outputJarPath = "${tempDir.absolutePath}/output.jar"

        val classpath = listOf(
            compilerJar.absolutePath,
            trove4jJar.absolutePath,
            File(homeDir, "lib/kotlin-reflect.jar").absolutePath,
            File(homeDir, "lib/kotlin-stdlib.jar").absolutePath,
            File(homeDir, "lib/kotlin-script-runtime.jar").absolutePath,
            File(homeDir, "lib/kotlinx-coroutines-core-jvm.jar").absolutePath,
            File(homeDir, "lib/annotations.jar").absolutePath
        ).joinToString(File.pathSeparator)

        val javaExe = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
        val pb = ProcessBuilder(
            javaExe, "-cp", classpath,
            "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
            sourceFile.absolutePath,
            "-d", outputJarPath,
            "-kotlin-home", homeDir.absolutePath,
            "-Xskip-prerelease-check"
        )
        val process = pb.start()
        val stdErrStr = process.errorStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()

        val finalExitCode = if (exitCode == 0) 0 else 1
        val compiledFilePath = if (finalExitCode == 0) outputJarPath else null

        return CompilerExecutionOutput(
            version = version,
            exitCode = finalExitCode,
            stdout = "",
            stderr = stdErrStr,
            compiledFilePath = compiledFilePath
        )
    }
}