rootProject.name = "JVM-MCL1"
include(
    "JVM-MCL",
    "JVM-MCLCore",
    "JVM-MCLBoot"
)

// Map new module names to legacy directory names
project(":JVM-MCL").projectDir = file("JMCL")
project(":JVM-MCLCore").projectDir = file("JMCLCore")
project(":JVM-MCLBoot").projectDir = file("JMCLBoot")

val minecraftLibraries = listOf("JVM-MCLTransformerDiscoveryService", "JVM-MCLMultiMCBootstrap")
include(minecraftLibraries)

// Map new minecraft library names to legacy directories
project(":JVM-MCLTransformerDiscoveryService").projectDir = file("minecraft/libraries/JMCLTransformerDiscoveryService")
project(":JVM-MCLMultiMCBootstrap").projectDir = file("minecraft/libraries/JMCLMultiMCBootstrap")