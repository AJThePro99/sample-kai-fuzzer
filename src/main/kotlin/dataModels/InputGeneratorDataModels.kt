package dataModels

import java.util.UUID
data class FuzzInput(
    val id: UUID = UUID.randomUUID(),
    val sourceCode : String,
    val generatorId : String,   // This is to mark which input generator class is used to generate this FuzzInput()
    val seedUsed : Long? = 0L
)