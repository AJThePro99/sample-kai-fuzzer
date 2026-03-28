package modules.utility.sutHandlerUtility

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption


/*
    This utility tool downloads the version specific kotlin compiler embeddable and the stdlib when you provide a list of Strings
    of the compiler versions.

    Currently, it can only download jvm dependencies. Not for the other backends.
 */
class KotlinCompilerDownloaderJVM {
    suspend fun downloadDependency(
        groupId: String,
        artifactId: String,
        version: String
    ): File = withContext(Dispatchers.IO) {
        val jarName = "$artifactId-$version.jar"
        val groupPath = groupId.replace('.', '/')
        val urlBase = "https://repo1.maven.org/maven2/$groupPath/$artifactId/$version/$jarName"
        val localPath = Paths.get("compilers/$jarName").toFile()

        if (localPath.exists()) return@withContext localPath

        println("== Downloading $artifactId $version ==")
        localPath.parentFile.mkdirs()
        URL(urlBase).openStream().use { input ->
            Files.copy(input, localPath.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        return@withContext localPath
    }

    suspend fun getCompilers(version: String): Triple<File, File, File> {
        val compilerJar = downloadDependency("org.jetbrains.kotlin", "kotlin-compiler-embeddable", version)
        val trove4jJar = downloadDependency("org.jetbrains.intellij.deps", "trove4j", "1.0.20181211")

        val homeDir = File("compilers/home-$version")
        val libDir = File(homeDir, "lib")
        if (!libDir.exists() || (libDir.listFiles()?.size ?: 0) < 3) {
            libDir.mkdirs()
            val stdlibJar = downloadDependency("org.jetbrains.kotlin", "kotlin-stdlib", version)
            val scriptRuntimeJar = downloadDependency("org.jetbrains.kotlin", "kotlin-script-runtime", version)
            val reflectJar = downloadDependency("org.jetbrains.kotlin", "kotlin-reflect", version)

            withContext(Dispatchers.IO) {
                Files.copy(
                    stdlibJar.toPath(),
                    File(libDir, "kotlin-stdlib.jar").toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )

                Files.copy(
                    scriptRuntimeJar.toPath(),
                    File(libDir, "kotlin-script-runtime.jar").toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
                Files.copy(
                    reflectJar.toPath(),
                    File(libDir, "kotlin-reflect.jar").toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }

        return Triple(compilerJar, trove4jJar, homeDir)
    }
}