package modules.inputGenerator

import baseInterfaces.KaiInputGenerator
import dataModels.FuzzInput
import kotlin.random.Random

/*
    This sample input generator will feed in one random Kotlin program (from a list of pre-written programs)
    And, send them with a random delay (1 - 3s each time).

    This is to emulate the working of a generator.
 */
class SampleInputGenerator : KaiInputGenerator {
    companion object {
    val programList: List<String> = listOf(
        "fun main() { println(\"Hello, Kotlin!\") }",
        "val sum = { a: Int, b: Int -> a + b }; fun main() { println(sum(5, 10)) }",
        "data class User(val id: Int, val name: String)",
        "fun main() { val list = listOf(1, 2, 3).map { it * 2 }; println(list) }",
        "class MyClass { val greeting = \"Hi\"; fun sayHi() = println(greeting) }",
        "fun checkValue(x: Int) = when(x) { 1 -> \"One\"; else -> \"Other\" }",
        "fun String.isLong(): Boolean = this.length > 10",
        "interface Drawable { fun draw() }; class Circle : Drawable { override fun draw() = println(\"O\") }",
        "fun main() { val name: String? = null; println(name?.length ?: 0) }",
        "import kotlin.math.*; fun main() { println(sqrt(16.0)) }"
    )
}

    // This function returns a random Kotlin program from the above list, and passes it to the orchestrator

    override suspend fun generateInput(): FuzzInput {
        return FuzzInput(
            sourceCode = programList.random(),
            generatorId = "SampleInputGenerator", // name of the generator class used
            seedUsed = Random.nextLong(0, 100)
        )
    }
}