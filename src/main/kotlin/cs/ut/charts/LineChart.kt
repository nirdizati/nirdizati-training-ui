package cs.ut.charts

import cs.ut.util.NirdizatiUtil
import org.zkoss.zk.ui.util.Clients

class LineChart(name: String, payload: String, private val numberOfEvents: Int) : Chart(name, payload) {
    override fun render() {
        Clients.evalJavaScript(
                "plot_line('$payload','${NirdizatiUtil.localizeText(getCaption())}','$numberOfEvents', '${name.toUpperCase() + if (name in normalized) "" else " (days)"}')")
    }
}