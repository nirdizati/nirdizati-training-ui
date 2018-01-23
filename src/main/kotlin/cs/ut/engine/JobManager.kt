package cs.ut.engine

import cs.ut.engine.events.*
import cs.ut.jobs.Job
import cs.ut.jobs.JobStatus
import cs.ut.jobs.SimulationJob
import cs.ut.util.*
import org.apache.log4j.Logger
import java.io.File
import java.util.concurrent.Future


object JobManager {
    val log: Logger = Logger.getLogger(JobManager::class.java)!!

    private val executedJobs: MutableMap<String, MutableList<Job>> = mutableMapOf()
    private val subscribers: MutableList<Any> = mutableListOf()
    private val jobStatus: MutableMap<Job, Future<*>> = mutableMapOf()

    fun subscribe(caller: Any) {
        synchronized(subscribers) {
            log.debug("New subscriber for updates -> ${caller::class.java}")
            subscribers.add(caller)
        }
    }

    fun unsubscribe(caller: Any) {
        synchronized(subscribers) {
            log.debug("Removing subscriber ${caller::class.java}")
            subscribers.remove(caller)
        }
    }

    fun statusUpdated(job: Job) {
        log.debug("Update event: ${job.id} -> notifying subscribers")
        handleEvent(StatusUpdateEvent(executedJobs.entries.firstOrNull { it.value.contains(job) }?.key ?: "", job))
    }

    private fun handleEvent(event: NirdizatiEvent) {
        val deadSubs = mutableListOf<Any>()
        synchronized(subscribers) {
            subscribers.forEach {
                if (isAlive(it)) {
                    notify(it, event)
                } else {
                    deadSubs.add(it)
                }
            }
        }
        deadSubs.cleanSubscribers()
    }

    private fun isAlive(obj: Any): Boolean {
        return findAliveCheck(obj::class.java).invoke(obj) as Boolean
    }

    private fun notify(obj: Any, event: NirdizatiEvent) {
        findCallback(obj::class.java, event::class).invoke(obj, event)
    }

    private fun List<Any>.cleanSubscribers() {
        log.debug("Received ${this.size} to remove from subscribers")
        this.forEach {
            unsubscribe(it)
        }
    }

    fun deployJobs(key: String, jobs: List<Job>) {
        log.debug("Jobs to be executed for client $key -> $jobs")

        val deployed: MutableList<Job> = executedJobs[key]?.toMutableList() ?: mutableListOf()
        log.debug("Client $key has ${deployed.size} completed jobs")
        log.debug("Deploying ${jobs.size} jobs")

        synchronized(jobStatus) {
            jobs.forEach {
                jobStatus[it] = NirdizatiThreadPool.execute(it)
                deployed.add(it)
            }
        }

        log.debug("Updating completed job status for $key")
        executedJobs[key] = deployed
        log.debug("Successfully updated $key -> $deployed")


        log.debug("Successfully deployed all jobs to worker")
        handleEvent(DeployEvent(key, jobs))
    }

    fun stopJob(job: Job) {
        log.debug("Stopping job ${job.id}")
        job.beforeInterrupt()
        log.debug("Completed before interrupt")
        jobStatus[job]?.cancel(true)
        log.debug("Job thread ${job.id} successfully interrupted")
    }

    fun runServiceJob(job: Job): Future<*> {
        log.debug("Running service job $job")
        return NirdizatiThreadPool.execute(job)
    }

    fun getJobsForKey(key: String) = executedJobs[key]

    fun removeJob(key: String, simulationJob: SimulationJob) {
        synchronized(executedJobs) {
            log.debug("Removing job $simulationJob for client $key")
            executedJobs[key]?.remove(simulationJob)
        }
    }

    fun loadJobsFromStorage(key: String): List<SimulationJob> {
        return mutableListOf<SimulationJob>().also { c ->
            LogManager.loadJobIds(key)
                .filter { data ->
                    val sessionJobs = executedJobs[key] ?: listOf<Job>()
                    data.id !in sessionJobs.map { it.id }
                            || sessionJobs.firstOrNull { it.id == data.id }?.status == JobStatus.COMPLETED
                }
                .forEach {
                    val params = readTrainingJson(it.id).flatMap { it.value }
                    c.add(SimulationJob(
                        params.first { it.type == ENCODING },
                        params.first { it.type == BUCKETING },
                        params.first { it.type == LEARNER },
                        params.first { it.type == PREDICTIONTYPE },
                        File(it.path),
                        key,
                        it.id
                    ).also { it.status = JobStatus.COMPLETED })
                }
        }
    }
}