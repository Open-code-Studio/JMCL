import org.Open_code_Studio.jmcl.gradle.TerracottaConfigUpgradeTask
import org.Open_code_Studio.jmcl.gradle.ci.GitHubActionUtils
import org.Open_code_Studio.jmcl.gradle.ci.JenkinsUtils
import org.Open_code_Studio.jmcl.gradle.l10n.CheckTranslations
import org.Open_code_Studio.jmcl.gradle.l10n.CreateLanguageList
import org.Open_code_Studio.jmcl.gradle.l10n.CreateLocaleNamesResourceBundle
import org.Open_code_Studio.jmcl.gradle.l10n.UpsideDownTranslate
import org.Open_code_Studio.jmcl.gradle.mod.ParseModDataTask
import org.Open_code_Studio.jmcl.gradle.pack.CreateDeb
import org.Open_code_Studio.jmcl.gradle.pack.ReleaseType
import org.Open_code_Studio.jmcl.gradle.utils.PropertiesUtils
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.zip.ZipFile

         
                              
 

val projectConfig = PropertiesUtils.load(rootProject.file("config/project.properties").toPath())

val isOfficial = JenkinsUtils.IS_ON_CI || GitHubActionUtils.IS_ON_OFFICIAL_REPO

val versionType = System.getenv("VERSION_TYPE") ?: if (isOfficial) "nightly" else "unofficial"
val versionRoot = System.getenv("VERSION_ROOT") ?: projectConfig.getProperty("versionRoot") ?: "3"

val microsoftAuthId = System.getenv("MICROSOFT_AUTH_ID") ?: getOrPromptMicrosoftAuthId()
val curseForgeApiKey = System.getenv("CURSEFORGE_API_KEY") ?: ""

val launcherExe = System.getenv("JVM-MCL_LAUNCHER_EXE") ?: ""

val buildNumber = System.getenv("BUILD_NUMBER")?.toInt()
if (buildNumber != null) {
    version = "$versionRoot.$buildNumber"
} else {
    val shortCommit = System.getenv("GITHUB_SHA")?.lowercase()?.substring(0, 7)
    version = if (shortCommit.isNullOrBlank()) {
        versionRoot
    } else {
        "$versionRoot-$shortCommit"
    }
}

                                                               
                                                              
fun readProperty(file: java.io.File, key: String): String? {
    if (!file.exists()) return null
    try {
        val lines = Files.readAllLines(file.toPath())
        for (l in lines) {
            val trimmed = l.trim()
            if (!trimmed.startsWith("#") && trimmed.startsWith("$key=")) {
                return trimmed.substring(key.length + 1)
            }
        }
    } catch (e: Exception) {
                 
    }
    return null
}

                                                        
fun writeProperty(file: java.io.File, key: String, value: String) {
    file.parentFile.mkdirs()
    val content = listOf(
        "# JMCL Azure AD / Microsoft OAuth Client ID",
        "# This file is git-ignored. Do not commit.",
        "$key=$value"
    )
    Files.write(file.toPath(), content)
}

                                                                                                    
fun readConsoleLine(prompt: String?): String? {
    val console: java.io.Console? = System.console()
    if (console != null) {
        return if (prompt != null) console.readLine(prompt) else console.readLine()
    }
                                                          
    if (prompt != null) {
        print(prompt)
    }
    return readLine()
}

                                                                 
                                                                                                  
                                                                                        
fun getOrPromptMicrosoftAuthId(): String {
    val configFile = rootProject.file("config/azure-ad.properties")

                                             
    if (configFile.exists()) {
        val id: String? = readProperty(configFile, "microsoft.auth.id")
        if (!id.isNullOrBlank()) {
            logger.quiet("Azure AD client ID loaded from {}", configFile)
            return id
        }
    }

                                
    logger.quiet("")
    logger.quiet("╔══════════════════════════════════════════════════════════════╗")
    logger.quiet("║  Azure AD / Microsoft OAuth Client ID Configuration        ║")
    logger.quiet("║                                                              ║")
    logger.quiet("║  Register an app at:                                        ║")
    logger.quiet("║  https://portal.azure.com/#view/Microsoft_AAD_RegisteredApps ║")
    logger.quiet("║                                                              ║")
    logger.quiet("║  Required: Redirect URI = http://localhost:29111/auth-response║")
    logger.quiet("║  Required: Live SDK support enabled                         ║")
    logger.quiet("╚══════════════════════════════════════════════════════════════╝")
    logger.quiet("")

    val clientId: String? = readConsoleLine("Enter your Azure AD client ID (or press Enter to skip): ")?.trim()

    if (!clientId.isNullOrBlank()) {
                                    
        try {
            writeProperty(configFile, "microsoft.auth.id", clientId)
            logger.quiet("Azure AD client ID saved to {}", configFile)
        } catch (e: Exception) {
            logger.warn("Failed to save client ID to {}: {}", configFile, e.message)
        }
        return clientId
    }

    logger.quiet("No client ID provided. Microsoft login will not be available in this build.")
    return ""
}

val embedResources by configurations.registering

dependencies {
    implementation(project(":JVM-MCLCore"))
    implementation(project(":JVM-MCLBoot"))
    implementation("libs:JFoenix")
    implementation(libs.jwebp)
    implementation(libs.fxsvgimage)
    implementation(libs.java.info)
    implementation(libs.monet.fx)
    implementation(libs.nayuki.qrcodegen)

    if (launcherExe.isBlank()) {
        implementation(libs.hmclauncher)
    }

    embedResources(libs.authlib.injector)
    embedResources(libs.lwjgl.unsafe.agent)
}

fun digest(algorithm: String, bytes: ByteArray): ByteArray = MessageDigest.getInstance(algorithm).digest(bytes)

fun createChecksum(file: File) {
    val algorithms = linkedMapOf(
        "SHA-1" to "sha1",
        "SHA-256" to "sha256",
        "SHA-512" to "sha512"
    )

    algorithms.forEach { (algorithm, ext) ->
        File(file.parentFile, "${file.name}.$ext").writeText(
            digest(algorithm, file.readBytes()).joinToString(separator = "", postfix = "\n") { "%02x".format(it) }
        )
    }
}

fun attachSignature(jar: File) {
    val keyLocation = System.getenv("JVM-MCL_SIGNATURE_KEY")
    if (keyLocation == null) {
        logger.warn("Missing signature key")
        return
    }

    val privatekey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(File(keyLocation).readBytes()))
    val signer = Signature.getInstance("SHA512withRSA")
    signer.initSign(privatekey)
    ZipFile(jar).use { zip ->
        zip.stream()
            .sorted(Comparator.comparing { it.name })
            .filter { it.name != "META-INF/jmcl_signature" }
            .forEach {
                signer.update(digest("SHA-512", it.name.toByteArray()))
                signer.update(digest("SHA-512", zip.getInputStream(it).readBytes()))
            }
    }
    val signature = signer.sign()
    FileSystems.newFileSystem(URI.create("jar:" + jar.toURI()), emptyMap<String, Any>()).use { zipfs ->
        Files.newOutputStream(zipfs.getPath("META-INF/jmcl_signature")).use { it.write(signature) }
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

tasks.checkstyleMain {
                                      
    exclude("**/org/Open_code_Studio/jmcl/ui/image/apng/**")
}

val addOpens = listOf(
    "java.base/java.lang",
    "java.base/java.lang.reflect",
    "java.base/jdk.internal.loader",
    "javafx.base/com.sun.javafx.binding",
    "javafx.base/com.sun.javafx.event",
    "javafx.base/com.sun.javafx.runtime",
    "javafx.base/javafx.beans.property",
    "javafx.graphics/javafx.css",
    "javafx.graphics/javafx.stage",
    "javafx.graphics/javafx.scene",
    "javafx.graphics/com.sun.glass.ui",
    "javafx.graphics/com.sun.javafx.scene",
    "javafx.graphics/com.sun.javafx.stage",
    "javafx.graphics/com.sun.javafx.util",
    "javafx.graphics/com.sun.prism",
    "javafx.controls/com.sun.javafx.scene.control",
    "javafx.controls/com.sun.javafx.scene.control.behavior",
    "javafx.graphics/com.sun.javafx.tk.quantum",
    "javafx.controls/javafx.scene.control.skin",
    "jdk.attach/sun.tools.attach",
)

tasks.compileJava {
    options.compilerArgs.addAll(addOpens.map { "--add-exports=$it=ALL-UNNAMED" })
}

val jmclProperties = buildList {
    add("jvmmcl.version" to project.version.toString())
    add("jvmmcl.add-opens" to addOpens.joinToString(" "))
    System.getenv("GITHUB_SHA")?.let {
        add("jvmmcl.version.hash" to it)
    }
    add("jvmmcl.version.type" to versionType)
    add("jvmmcl.microsoft.auth.id" to microsoftAuthId)
    add("jvmmcl.curseforge.apikey" to curseForgeApiKey)
    add("jvmmcl.authlib-injector.version" to libs.authlib.injector.get().version!!)
    add("jvmmcl.lwjgl-unsafe-agent.version" to libs.lwjgl.unsafe.agent.get().version!!)
}

val jmclPropertiesFile = layout.buildDirectory.file("jvmmcl.properties")
val createPropertiesFile by tasks.registering {
    outputs.file(jmclPropertiesFile)
    jmclProperties.forEach { (k, v) -> inputs.property(k, v) }

    doLast {
        val targetFile = jmclPropertiesFile.get().asFile
        targetFile.parentFile.mkdir()
        targetFile.bufferedWriter().use {
            for ((k, v) in jmclProperties) {
                it.write("$k=$v\n")
            }
        }
    }
}

tasks.jar {
    enabled = false
    dependsOn(tasks["shadowJar"])
}

val jarPath = tasks.jar.get().archiveFile.get().asFile

tasks.shadowJar {
    dependsOn(createPropertiesFile)

    archiveClassifier.set(null as String?)

    exclude("**/package-info.class")
    exclude("META-INF/maven/**")

    exclude("META-INF/services/javax.imageio.spi.ImageReaderSpi")
    exclude("META-INF/services/javax.imageio.spi.ImageInputStreamSpi")

    listOf(
        "aix-*", "sunos-*", "openbsd-*", "dragonflybsd-*", "freebsd-*", "linux-*",
        "*-ppc", "*-ppc64le", "*-s390x", "*-armel",
    ).forEach { exclude("com/sun/jna/$it/**") }

    minimize {
        exclude(dependency("com.google.code.gson:.*:.*"))
        exclude(dependency("net.java.dev.jna:jna:.*"))
        exclude(dependency("libs:JFoenix:.*"))
        exclude(project(":JVM-MCLBoot"))
    }

    manifest.attributes(
        "Created-By" to "Copyright(c) 2013-2025 Open Code Studio.",
        "Implementation-Version" to project.version.toString(),
        "Main-Class" to "org.Open_code_Studio.jmcl.Main",
        "Multi-Release" to "true",
        "Add-Opens" to addOpens.joinToString(" "),
        "Enable-Native-Access" to "ALL-UNNAMED",
        "Enable-Final-Field-Mutation" to "ALL-UNNAMED",
    )

    if (launcherExe.isNotBlank()) {
        into("assets") {
            from(file(launcherExe))
        }
    }

    doLast {
        attachSignature(jarPath)
        createChecksum(jarPath)
    }
}

tasks.processResources {
    dependsOn(createPropertiesFile)
    dependsOn(upsideDownTranslate)
    dependsOn(createLocaleNamesResourceBundle)
    dependsOn(createLanguageList)

    into("assets/") {
        from(jmclPropertiesFile)
        from(embedResources)
    }

    into("assets/lang") {
        from(createLanguageList.map { it.outputFile })
        from(upsideDownTranslate.map { it.outputFile })
        from(createLocaleNamesResourceBundle.map { it.outputDirectory })
    }

    inputs.property("terracotta_version", libs.versions.terracotta)
    doLast {
        upgradeTerracottaConfig.get().checkValid()
    }
}

fun artifactFile(ext: String) = jarPath.resolveSibling(jarPath.nameWithoutExtension + '.' + ext)

val makeExecutables by tasks.registering {
    dependsOn(tasks.jar)

    inputs.file(jarPath)
    outputs.file(artifactFile("sh"))
    outputs.file(artifactFile("exe"))

    doLast {
        val jarContent = jarPath.readBytes()

        ZipFile(jarPath).use { zipFile ->
            for (extension in listOf("exe", "sh")) {
                val output = artifactFile(extension)
                val entry = zipFile.getEntry("assets/JMCLauncher.$extension")
                    ?: zipFile.getEntry("assets/HMCLauncher.$extension")
                
                if (entry == null) {
                    logger.warn("Launcher.$extension not found, skipping")
                    continue
                }

                output.outputStream().use { outputStream ->
                    val launcherBytes = zipFile.getInputStream(entry).readBytes()
                    
                                                                         
                    val patchedBytes = if (extension == "exe") {
                        patchExeMetadata(launcherBytes)
                    } else {
                        launcherBytes
                    }
                    
                    outputStream.write(patchedBytes)
                    outputStream.write(jarContent)
                }

                createChecksum(output)
            }
        }
    }
}

                                                                                    
                                                                                 
private fun patchExeMetadata(data: ByteArray): ByteArray {
    val result = data.copyOf()
                              
    val hmcl = byteArrayOf(0x48, 0x00, 0x4D, 0x00, 0x43, 0x00, 0x4C, 0x00)           
    val jmcl = byteArrayOf(0x4A, 0x00, 0x4D, 0x00, 0x43, 0x00, 0x4C, 0x00)           
    val launcherOld = byteArrayOf(
        0x48, 0x00, 0x4D, 0x00, 0x43, 0x00, 0x4C, 0x00,           
        0x61, 0x00, 0x75, 0x00, 0x6E, 0x00, 0x63, 0x00, 0x68, 0x00, 0x65, 0x00, 0x72, 0x00                 
    )
    val launcherNew = byteArrayOf(
        0x4A, 0x00, 0x4D, 0x00, 0x43, 0x00, 0x4C, 0x00,           
        0x61, 0x00, 0x75, 0x00, 0x6E, 0x00, 0x63, 0x00, 0x68, 0x00, 0x65, 0x00, 0x72, 0x00                 
    )

    var i = 0
    while (i <= result.size - minOf(hmcl.size, launcherOld.size)) {
        when {
            result.matchesAt(i, launcherOld) -> {
                launcherNew.forEachIndexed { j, b -> result[i + j] = b }
                i += launcherOld.size
            }
            result.matchesAt(i, hmcl) -> {
                jmcl.forEachIndexed { j, b -> result[i + j] = b }
                i += hmcl.size
            }
            else -> i++
        }
    }
    return result
}

private fun ByteArray.matchesAt(offset: Int, pattern: ByteArray): Boolean {
    if (offset + pattern.size > size) return false
    for (i in pattern.indices) {
        if (this[offset + i] != pattern[i]) return false
    }
    return true
}

val makeDeb by tasks.registering(CreateDeb::class) {
    dependsOn(makeExecutables)

    val debFile = layout.file(provider { artifactFile("deb") })

    val debChannel = when (versionType) {
        "stable" -> ReleaseType.STABLE
        "dev" -> ReleaseType.DEVELOPMENT
        else -> ReleaseType.NIGHTLY
    }

    version.set(project.version.toString())
    releaseType.set(debChannel)
    appShFile.set(layout.file(provider { artifactFile("sh") }))
    iconFile.set(layout.projectDirectory.file("image/jmcl.png"))
    outputFile.set(debFile)

    doLast {
        createChecksum(debFile.get().asFile)
    }
}

tasks.build {
    dependsOn(makeExecutables)
    dependsOn(makeDeb)
}

fun parseToolOptions(options: String?): MutableList<String> {
    if (options == null)
        return mutableListOf()

    val builder = StringBuilder()
    val result = mutableListOf<String>()

    var offset = 0

    loop@ while (offset < options.length) {
        val ch = options[offset]
        if (Character.isWhitespace(ch)) {
            if (builder.isNotEmpty()) {
                result += builder.toString()
                builder.clear()
            }

            while (offset < options.length && Character.isWhitespace(options[offset])) {
                offset++
            }

            continue@loop
        }

        if (ch == '\'' || ch == '"') {
            offset++

            while (offset < options.length) {
                val ch2 = options[offset++]
                if (ch2 != ch) {
                    builder.append(ch2)
                } else {
                    continue@loop
                }
            }

            throw GradleException("Unmatched quote in $options")
        }

        builder.append(ch)
        offset++
    }

    if (builder.isNotEmpty()) {
        result += builder.toString()
    }

    return result
}

                                                                        
                                                                      
tasks.register("configureAzureAd") {
    group = "configuration"
    description = "Configure Azure AD / Microsoft OAuth client ID for Microsoft account login"

    doLast {
        val configFile = rootProject.file("config/azure-ad.properties")

        val currentId: String? = try {
            val fromFile = readProperty(configFile, "microsoft.auth.id")
            if (!fromFile.isNullOrBlank()) fromFile else System.getenv("MICROSOFT_AUTH_ID")
        } catch (e: Exception) { null }

        logger.quiet("")
        logger.quiet("╔══════════════════════════════════════════════════════════════╗")
        logger.quiet("║  Azure AD / Microsoft OAuth Client ID Configuration        ║")
        logger.quiet("║                                                              ║")
        logger.quiet("║  Register an app at:                                        ║")
        logger.quiet("║  https://portal.azure.com/#view/Microsoft_AAD_RegisteredApps ║")
        logger.quiet("║                                                              ║")
        logger.quiet("║  Required: Redirect URI = http://localhost:29111/auth-response║")
        logger.quiet("║  Required: Live SDK support enabled                         ║")
        logger.quiet("╚══════════════════════════════════════════════════════════════╝")
        logger.quiet("")

        if (currentId != null) {
            logger.quiet("Current client ID: {}", currentId)
        } else {
            logger.quiet("No client ID currently configured.")
        }
        logger.quiet("")

        val clientId: String? = readConsoleLine("Enter new Azure AD client ID (or press Enter to keep current): ")?.trim()

        if (!clientId.isNullOrBlank()) {
            try {
                writeProperty(configFile, "microsoft.auth.id", clientId)
                logger.quiet("Azure AD client ID saved to {}", configFile)
                logger.quiet("Run './gradlew -g .gradle-user-home clean build' to rebuild with the new client ID.")
            } catch (e: Exception) {
                throw GradleException("Failed to save client ID: ${e.message}", e)
            }
        } else {
            logger.quiet("Client ID unchanged.")
        }
    }
}

                    
tasks.withType<JavaExec> {
    if (name != "run") {
        jvmArgs(addOpens.map { "--add-opens=$it=ALL-UNNAMED" })
                                                      
                                                           
           
    }
}

tasks.register<JavaExec>("run") {
    dependsOn(tasks.jar)

    group = "application"

    classpath = files(jarPath)
    workingDir = rootProject.rootDir

    val vmOptions = parseToolOptions(System.getenv("JVM-MCL_JAVA_OPTS") ?: "-Xmx1g")
    if (vmOptions.none { it.startsWith("-Djvmmcl.offline.auth.restricted=") })
        vmOptions += "-Djvmmcl.offline.auth.restricted=false"

    jvmArgs(vmOptions)

    val jmclJavaHome = System.getenv("JVM-MCL_JAVA_HOME")
    if (jmclJavaHome != null) {
        this.executable(
            file(jmclJavaHome).resolve("bin")
                .resolve(if (System.getProperty("os.name").lowercase().startsWith("windows")) "java.exe" else "java")
        )
    }

    doFirst {
        logger.quiet("JVM-MCL_JAVA_OPTS: {}", vmOptions)
        logger.quiet("JVM-MCL_JAVA_HOME: {}", jmclJavaHome ?: System.getProperty("java.home"))
    }
}

             

val upgradeTerracottaConfig = tasks.register<TerracottaConfigUpgradeTask>("upgradeTerracottaConfig") {
    val destination = layout.projectDirectory.file("src/main/resources/assets/terracotta.json")
    val source = layout.projectDirectory.file("terracotta-template.json");

    classifiers.set(
        listOf(
            "windows-x86_64", "windows-arm64",
            "macos-x86_64", "macos-arm64",
            "linux-x86_64", "linux-arm64", "linux-loongarch64", "linux-riscv64",
            "freebsd-x86_64"
        )
    )

    version.set(libs.versions.terracotta)
    downloadURL.set($$"https://github.com/burningtnt/Terracotta/releases/download/v${version}/terracotta-${version}-${classifier}-pkg.tar.gz")

    templateFile.set(source)
    outputFile.set(destination)
}

                     

tasks.register<CheckTranslations>("checkTranslations") {
    val dir = layout.projectDirectory.dir("src/main/resources/assets/lang")

    englishFile.set(dir.file("I18N.properties"))
    simplifiedChineseFile.set(dir.file("I18N_zh_CN.properties"))
    traditionalChineseFile.set(dir.file("I18N_zh.properties"))
    classicalChineseFile.set(dir.file("I18N_lzh.properties"))
}

       

val generatedDir = layout.buildDirectory.dir("generated")

val upsideDownTranslate by tasks.registering(UpsideDownTranslate::class) {
    inputFile.set(layout.projectDirectory.file("src/main/resources/assets/lang/I18N.properties"))
    outputFile.set(generatedDir.map { it.file("generated/i18n/I18N_en_Qabs.properties") })
}

val createLanguageList by tasks.registering(CreateLanguageList::class) {
    resourceBundleDir.set(layout.projectDirectory.dir("src/main/resources/assets/lang"))
    resourceBundleBaseName.set("I18N")
    additionalLanguages.set(listOf("en-Qabs"))
    outputFile.set(generatedDir.map { it.file("languages.json") })
}

val createLocaleNamesResourceBundle by tasks.registering(CreateLocaleNamesResourceBundle::class) {
    dependsOn(createLanguageList)

    languagesFile.set(createLanguageList.flatMap { it.outputFile })
    outputDirectory.set(generatedDir.map { it.dir("generated/LocaleNames") })
}

             

tasks.register<ParseModDataTask>("parseModData") {
    inputFile.set(layout.projectDirectory.file("mod.json"))
    outputFile.set(layout.projectDirectory.file("src/main/resources/assets/mod_data.txt"))
}

tasks.register<ParseModDataTask>("parseModPackData") {
    inputFile.set(layout.projectDirectory.file("modpack.json"))
    outputFile.set(layout.projectDirectory.file("src/main/resources/assets/modpack_data.txt"))
}