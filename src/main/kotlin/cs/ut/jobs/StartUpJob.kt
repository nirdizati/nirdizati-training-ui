package cs.ut.jobs

import cs.ut.config.MasterConfiguration
import cs.ut.config.nodes.Dir
import cs.ut.exceptions.NirdizatiRuntimeException
import java.io.File

class StartUpJob : Job() {

    private val config = MasterConfiguration.dirConfig
    private val userConf = MasterConfiguration.userPreferences

    override fun execute() {
        val start = System.currentTimeMillis()
        Dir.values().forEach {
            if (it == Dir.PYTHON) return@forEach
            config.dirByName(it).prepareDirectory()
        }

        val end = System.currentTimeMillis()
        log.debug("Finished directory preparation in ${end - start} ms")
    }

    private fun File.prepareDirectory() {
        log.debug("Preparing ${this.absolutePath}")
        if (!this.exists()) {
            log.debug("$this does not exist, creating directory")
            if (!this.mkdirs()) {
                log.error("No rights to create dir")
                throw NirdizatiRuntimeException("Cannot create dir. Am I run as root?")
            }
        } else {
            log.debug("Directory exists")
            if (!this.isDirectory) {
                log.error("$this is not a directory")
                throw NirdizatiRuntimeException("Delete the file or change $this in configuration")
            }
        }

        log.debug("$this is a dir")
        if (userConf.enabled) {
            log.debug("User rights change enabled -> applying new user rights")
            UserRightsJob.updateACL(this)
        }
        log.debug("Finished preparing ${this.absolutePath}")
    }
}