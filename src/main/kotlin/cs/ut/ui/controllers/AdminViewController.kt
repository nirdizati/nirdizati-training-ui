package cs.ut.ui.controllers

import com.lowagie.text.pdf.codec.Base64
import cs.ut.configuration.ConfigurationReader
import cs.ut.engine.NirdizatiThreadPool
import cs.ut.logging.NirdizatiLogger
import cs.ut.providers.Dir
import cs.ut.providers.DirectoryConfiguration
import cs.ut.ui.UIComponent
import org.zkoss.util.resource.Labels
import org.zkoss.zk.ui.Component
import org.zkoss.zk.ui.event.Events
import org.zkoss.zk.ui.select.SelectorComposer
import org.zkoss.zk.ui.select.annotation.Wire
import org.zkoss.zul.Button
import org.zkoss.zul.Textbox
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.charset.Charset

class AdminViewController : SelectorComposer<Component>(), UIComponent {
    val log = NirdizatiLogger.getLogger(AdminViewController::class.java, getSessionId())

    @Wire
    private lateinit var flushConfig: Button

    @Wire
    private lateinit var flushMessages: Button

    @Wire
    private lateinit var killThreadPool: Button

    @Wire
    private lateinit var restartThreadPool: Button

    @Wire
    private lateinit var passwordField: Textbox

    @Wire
    private lateinit var showLogs: Button

    @Wire
    private lateinit var logData: Textbox

    private val configNode = ConfigurationReader.findNode("userPreferences/adminFunctionality")

    private val logFile: File = File(DirectoryConfiguration.dirPath(Dir.LOG_FILE))

    override fun doAfterCompose(comp: Component?) {
        super.doAfterCompose(comp)

        flushConfig.addEventListener(Events.ON_CLICK, { _ ->
            log.debug("Flushing master configuration")
            ConfigurationReader.reload()
            log.debug("Master configuration flushed")
        })

        flushMessages.addEventListener(Events.ON_CLICK, { _ ->
            performTask(flushMessages())
        })

        killThreadPool.addEventListener(Events.ON_CLICK, { _ ->
            performTask(killThreadPool())
        })

        restartThreadPool.addEventListener(Events.ON_CLICK, { _ ->
            performTask(restartThreadPool())
        })

        showLogs.addEventListener(Events.ON_CLICK, { _ ->
            performTask(readLogFile())
        })
    }

    private fun performTask(task: Runnable) {
        if (isAuthorized()) {
            passwordField.errorMessage = ""
            task.run()
        } else {
            log.debug("Not authorized: ${passwordField.value}")
            passwordField.errorMessage = "Invalid key"
        }
    }

    private fun restartThreadPool(): Runnable = Runnable {
        log.debug("Restarting threadpool")
        NirdizatiThreadPool.restart()
        log.debug("Successfully restarted threadpool")
    }


    private fun killThreadPool(): Runnable = Runnable {
        log.debug("Killing threadpool")
        NirdizatiThreadPool.shutDown()
        log.debug("Successfully killed threadpool")
    }


    private fun flushMessages(): Runnable = Runnable {
        log.debug("Flushing messages files")
        Labels.reset()
        log.debug("Successfully flushed messages file")
    }

    private fun readLogFile(): Runnable = Runnable {
        if (logFile.exists() && logFile.isFile) {
            logData.isVisible = true
            log.debug("File exists and is a log file")
            logData.value = BufferedReader(FileReader(logFile)).lineSequence().joinToString("\n")
            log.debug("Finished parsing log file")
        }
    }

    private fun isAuthorized(): Boolean {
        return configNode.isEnabled() && (!configNode.valueWithIdentifier("isPasswordRequired").booleanValue() ||
                Base64.encodeBytes(
                    (passwordField.value ?: "").toByteArray(Charset.forName("UTF-8"))
                ) == configNode.valueWithIdentifier("password").value)
    }

}