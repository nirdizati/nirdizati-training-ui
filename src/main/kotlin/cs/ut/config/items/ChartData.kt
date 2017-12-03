package cs.ut.config.items

import com.google.gson.Gson
import cs.ut.jobs.SimulationJob
import cs.ut.manager.LogManager
import cs.ut.ui.Mode
import cs.ut.ui.getLinearPayload
import org.zkoss.util.resource.Labels
import org.zkoss.zk.ui.event.Event
import org.zkoss.zk.ui.event.SerializableEventListener
import org.zkoss.zk.ui.util.Clients

enum class ChartType {
    scatter,
    line,
    bar
}

class ChartData(val caption: String, val action: SerializableEventListener<Event>)

class ChartDataDelegate(val job: SimulationJob?) {
    private val namespace = "chart_data."
    private val gson = Gson()

    fun getCharts(): List<ChartData> = if (job == null) listOf() else listOf(
            /* True vs predicted value */
            ChartData(namespace + "true_vs_predicted", SerializableEventListener { _ ->
                val payload = getLinearPayload(LogManager.getInstance().getDetailedFile(job), Mode.SCATTER)
                val caption = namespace + "true_vs_predicted"
                evalJs(gson.toJson(payload), caption, ChartType.scatter)
            }),

            /* Number of events vs MAE */
            ChartData(namespace + "number_vs_mae", SerializableEventListener { _ ->
                val payload = getLinearPayload(LogManager.getInstance().getValidationFile(job), Mode.LINE)
                val caption = namespace + "number_vs_mae"
                evalJs(gson.toJson(payload), caption, ChartType.line, payload.last().x.toInt())
            })
    )

    private fun evalJs(payload: String, caption: String, type: ChartType, events: Int = 0) {
        when (type) {
            ChartType.scatter -> Clients.evalJavaScript("plot_scatter('$payload','${Labels.getLabel(caption)}')")
            ChartType.line -> Clients.evalJavaScript("plot_line('$payload','${Labels.getLabel(caption)}','$events')")
            ChartType.bar -> Clients.evalJavaScript("plot_bar('$payload','${Labels.getLabel(caption)}')")
        }
    }
}