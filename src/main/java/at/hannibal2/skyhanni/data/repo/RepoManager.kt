package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.features.dev.RepositoryConfig
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import com.google.gson.JsonObject
import net.minecraft.util.IChatComponent
import org.apache.commons.io.FileUtils
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object RepoManager {

    private val gson get() = ConfigManager.gson
    private val configLocation = ConfigManager.configDirectory
    val repoLocation: File = File(configLocation, "repo")
    private var error = false
    private var lastRepoUpdate = SimpleTimeMark.now()
    private var repoDownloadFailed = false

    private val config get() = SkyHanniMod.feature.dev.repo

    val successfulConstants = mutableListOf<String>()
    val unsuccessfulConstants = mutableListOf<String>()
    var usingBackupRepo = false

    private var lastConstant: String? = null

    fun setLastConstant(constant: String) {
        lastConstant?.let {
            successfulConstants.add(it)
        }
        lastConstant = constant
    }

    fun getRepoLocation(): String {
        return "${config.location.user}/${config.location.name}/${config.location.branch}"
    }

    private const val DEFAULT_USER = "hannibal002"
    private const val DEFAULT_NAME = "SkyHanni-REPO"
    private const val DEFAULT_BRANCH = "main"

    fun RepositoryConfig.RepositoryLocation.hasDefaultSettings() =
        user.lowercase() == DEFAULT_USER.lowercase() &&
            name.lowercase() == DEFAULT_NAME.lowercase() &&
            branch.lowercase() == DEFAULT_BRANCH.lowercase()

    fun initRepo() {
        shouldManuallyReload = true
        SkyHanniMod.launchIOCoroutine {
            if (config.repoAutoUpdate) {
                fetchRepository(command = false)
                if (repoDownloadFailed) {
                    switchToBackupRepo()
                }
            }
            reloadRepository()
        }
    }

    private var shouldManuallyReload = false

    private var currentlyFetching = false

    @JvmStatic
    fun updateRepo() {
        shouldManuallyReload = true
        checkRepoLocation()
        SkyHanniMod.launchIOCoroutine {
            fetchRepository(command = true)
            reloadRepository("Repo updated successfully.")
            if (unsuccessfulConstants.isNotEmpty() || usingBackupRepo) {
                if (!ErrorManager.logErrorStateWithData(
                        "Error updating reading Sh Repo",
                        "no success",
                        "usingBackupRepo" to usingBackupRepo,
                        "unsuccessfulConstants" to unsuccessfulConstants,
                    )
                ) {
                    ChatUtils.chat("§cFailed to load the repo! See above for more infos.")
                }
                return@launchIOCoroutine
            }
        }
    }

    fun reloadLocalRepo() {
        shouldManuallyReload = true
        SkyHanniMod.launchIOCoroutine {
            reloadRepository("Repo loaded from local files successfully.")
        }
    }

    private fun fetchRepository(command: Boolean) {
        if (currentlyFetching) return
        currentlyFetching = true
        doTheFetching(command)
        currentlyFetching = false
    }

    private fun doTheFetching(command: Boolean) {
        try {
            val currentDownloadedCommit = readCurrentCommit()
            var latestRepoCommit: String? = null
            try {
                InputStreamReader(URL(getCommitApiUrl()).openStream())
                    .use { inReader ->
                        val commits: JsonObject = gson.fromJson(inReader, JsonObject::class.java)
                        latestRepoCommit = commits["sha"].asString
                    }
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(
                    e,
                    "Error while loading data from repo",
                    "command" to command,
                    "currentDownloadedCommit" to currentDownloadedCommit,
                )
                repoDownloadFailed = true
            }

            if (repoLocation.exists() &&
                currentDownloadedCommit == latestRepoCommit &&
                unsuccessfulConstants.isEmpty() &&
                lastRepoUpdate.passedSince() < 1.minutes
            ) {
                if (command) {
                    ChatUtils.chat("§7The repo is already up to date!")
                    shouldManuallyReload = false
                }
                return
            }
            lastRepoUpdate = SimpleTimeMark.now()

            repoLocation.mkdirs()
            val itemsZip = File(repoLocation, "sh-repo-main.zip")
            itemsZip.createNewFile()

            val url = URL(getDownloadUrl(latestRepoCommit))
            val urlConnection = url.openConnection()
            urlConnection.connectTimeout = 15000
            urlConnection.readTimeout = 30000

            RepoUtils.recursiveDelete(repoLocation)
            repoLocation.mkdirs()

            try {
                urlConnection.getInputStream().use { `is` ->
                    FileUtils.copyInputStreamToFile(
                        `is`,
                        itemsZip,
                    )
                }
            } catch (e: IOException) {
                ErrorManager.logErrorWithData(
                    e,
                    "Failed to download SkyHanni Repo",
                    "url" to url,
                    "command" to command,
                )
                repoDownloadFailed = true
                return
            }
            RepoUtils.unzipIgnoreFirstFolder(
                itemsZip.absolutePath,
                repoLocation.absolutePath,
            )
            if (currentDownloadedCommit == null || currentDownloadedCommit != latestRepoCommit) {
                writeCurrentCommit(latestRepoCommit)
            }
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e,
                "Failed to download SkyHanni Repo",
                "command" to command,
            )
            repoDownloadFailed = true
        }
        repoDownloadFailed = false
        usingBackupRepo = false
    }

    private fun reloadRepository(answerMessage: String = "") {
        if (!shouldManuallyReload) return
        // TODO move away
        ErrorManager.resetCache()
        error = false
        successfulConstants.clear()
        unsuccessfulConstants.clear()
        lastConstant = null

        RepositoryReloadEvent(repoLocation, gson).post {
            error = true
            lastConstant?.let {
                unsuccessfulConstants.add(it)
            }
            lastConstant = null
        }
        if (answerMessage.isNotEmpty() && !error) {
            ChatUtils.chat("§a$answerMessage")
        }
        if (error) {
            ChatUtils.clickableChat(
                "Error with the repo detected, try /shupdaterepo to fix it!",
                onClick = {
                    updateRepo()
                },
                "§eClick to update the repo!",
                prefixColor = "§c",
            )
            if (unsuccessfulConstants.isEmpty()) {
                unsuccessfulConstants.add("All Constants")
            }
        }
    }

    private fun writeCurrentCommit(commit: String?) {
        val newCurrentCommitJSON = JsonObject()
        newCurrentCommitJSON.addProperty("sha", commit)
        try {
            writeJson(newCurrentCommitJSON, File(configLocation, "currentCommit.json"))
        } catch (ignored: IOException) {
        }
    }

    private fun readCurrentCommit(): String? {
        val currentCommitJSON: JsonObject? = getJsonFromFile(File(configLocation, "currentCommit.json"))
        return currentCommitJSON?.get("sha")?.asString
    }

    fun displayRepoStatus(joinEvent: Boolean) {
        if (joinEvent) {
            if (unsuccessfulConstants.isNotEmpty()) {
                val text = mutableListOf<IChatComponent>()
                text.add(
                    (
                        "§c[SkyHanni-${SkyHanniMod.VERSION}] §7Repo Issue! Some features may not work. " +
                            "Please report this error on the Discord!"
                        ).asComponent(),
                )
                text.add("§7Repo Auto Update Value: §c${config.repoAutoUpdate}".asComponent())
                text.add("§7Backup Repo Value: §c$usingBackupRepo".asComponent())
                text.add("§7If you have Repo Auto Update turned off, please try turning that on.".asComponent())
                text.add("§cUnsuccessful Constants §7(${unsuccessfulConstants.size}):".asComponent())

                for (constant in unsuccessfulConstants) {
                    text.add("   §e- §7$constant".asComponent())
                }
                TextHelper.multiline(text).send()
            }
            return
        }
        val currentCommit = readCurrentCommit()
        if (unsuccessfulConstants.isEmpty() && successfulConstants.isNotEmpty()) {
            ChatUtils.chat("Repo working fine! Commit hash: $currentCommit", prefixColor = "§a")
            return
        }
        ChatUtils.chat("Repo has errors! Commit hash: $currentCommit", prefixColor = "§c")
        if (successfulConstants.isNotEmpty()) ChatUtils.chat(
            "Successful Constants §7(${successfulConstants.size}):",
            prefixColor = "§a",
        )
        for (constant in successfulConstants) {
            ChatUtils.chat("   §a- §7$constant", false)
        }
        ChatUtils.chat("Unsuccessful Constants §7(${unsuccessfulConstants.size}):")
        for (constant in unsuccessfulConstants) {
            ChatUtils.chat("   §e- §7$constant", false)
        }
    }

    /**
     * Parses a file in to a JsonObject.
     */
    private fun getJsonFromFile(file: File?): JsonObject? {
        try {
            BufferedReader(
                InputStreamReader(
                    FileInputStream(file),
                    StandardCharsets.UTF_8,
                ),
            ).use { reader ->
                return gson.fromJson(reader, JsonObject::class.java)
            }
        } catch (e: java.lang.Exception) {
            return null
        }
    }

    private fun getCommitApiUrl(): String {
        val repoUser = config.location.user
        val repoName = config.location.name
        val repoBranch = config.location.branch
        return "https://api.github.com/repos/$repoUser/$repoName/commits/$repoBranch"
    }

    private fun getDownloadUrl(commitId: String?): String {
        val repoUser = config.location.user
        val repoName = config.location.name
        return "https://github.com/$repoUser/$repoName/archive/$commitId.zip"
    }

    @Throws(IOException::class)
    fun writeJson(json: JsonObject?, file: File) {
        file.createNewFile()
        BufferedWriter(
            OutputStreamWriter(
                FileOutputStream(file),
                StandardCharsets.UTF_8,
            ),
        ).use { writer -> writer.write(gson.toJson(json)) }
    }

    fun resetRepositoryLocation(manual: Boolean = false) {

        with(config.location) {
            if (hasDefaultSettings()) {
                if (manual) {
                    ChatUtils.chat("Repo settings are already on default!")
                }
                return
            }

            user = DEFAULT_USER
            name = DEFAULT_NAME
            branch = DEFAULT_BRANCH
            if (manual) {
                ChatUtils.clickableChat(
                    "Reset Repo settings to default. " +
                        "Click §aUpdate Repo Now §ein config or run /shupdaterepo to update!",
                    onClick = {
                        updateRepo()
                    },
                    "§eClick to update the repo!",
                )
            }
        }
    }

    private fun checkRepoLocation() {
        if (config.location.run { user.isEmpty() || name.isEmpty() || branch.isEmpty() }) {
            ChatUtils.userError("Invalid Repo settings detected, resetting default settings.")
            resetRepositoryLocation()
        }
    }

    // Code taken from NotEnoughUpdates
    private fun switchToBackupRepo() {
        usingBackupRepo = true
        println("Attempting to switch to backup repo")

        try {
            repoLocation.mkdirs()
            val destinationFile = File(repoLocation, "sh-repo-main.zip").apply { createNewFile() }
            val destinationPath = destinationFile.toPath()

            val inputStream = RepoManager::class.java.classLoader.getResourceAsStream("assets/skyhanni/repo.zip")
                ?: throw IOException("Failed to find backup repo")

            Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING)
            RepoUtils.unzipIgnoreFirstFolder(destinationPath.toAbsolutePath().toString(), repoLocation.absolutePath)
            writeCurrentCommit("backup-repo")

            println("Successfully switched to backup repo")
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Failed to switch to backup repo")
        }
    }
}
