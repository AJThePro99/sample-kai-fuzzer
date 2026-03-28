package modules.sutHandler

import baseInterfaces.KaiSutHandler
import dataModels.CompilerExecutionOutput
import dataModels.FuzzInput
import dataModels.SutBackend
import dataModels.SutResult
import modules.utility.sutHandlerUtility.KotlinCompilerDownloaderJVM
import modules.utility.sutHandlerUtility.OutputCaptureUtility
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
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

        val classLoader = URLClassLoader(
            arrayOf(compilerJar.toURI().toURL(), trove4jJar.toURI().toURL())
        )

        val captureResult = OutputCaptureUtility.capturePrintStream { printStream ->
            val compilerClass = classLoader.loadClass("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
            val compiler = compilerClass.getDeclaredConstructor().newInstance()

            /*
                 last flag suppresses errors that occur when a project uses dependencies or classes compiled with a pre-release
                 (e.g., alpha, beta) version of the Kotlin compiler that is newer than the project's own compiler version
             */
            val args = arrayOf(
                sourceFile.absolutePath,
                "-d", outputJarPath,
                "-kotlin-home", homeDir.absolutePath,
                "-Xskip-prerelease-check"
            )

            val execMethod = compilerClass.getMethod("exec", PrintStream::class.java, Array<String>::class.java)
            val exitCodeEnum = execMethod.invoke(compiler, printStream, args)

            val exitCodeName = exitCodeEnum.javaClass.getMethod("name").invoke(exitCodeEnum) as String
            if (exitCodeName == "OK") 0 else 1
        }

        val finalExitCode = if (captureResult.exception != null) 2 else (captureResult.result ?: 1)
        val compiledFilePath = if (finalExitCode == 0) outputJarPath else null

        return CompilerExecutionOutput(
            version = version,
            exitCode = finalExitCode,
            stdout = "",
            stderr = captureResult.output,
            compiledFilePath = compiledFilePath
        )
    }
}