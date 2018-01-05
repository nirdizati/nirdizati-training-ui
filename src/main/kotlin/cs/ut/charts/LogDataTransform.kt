package cs.ut.charts

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

const val ACTUAL = "actual"
const val PREDICTED = "predicted"
const val NR_EVENTS = "nr_events"
const val SCORE = "score"
const val delim = ","
const val METRIC = "metric"
const val MAE = "mae"

class LinearData(val x: Float, val y: Float, val dataType: String)

enum class Mode {
    SCATTER,
    LINE
}

fun getLinearPayload(file: File, mode: Mode): List<LinearData> {
    val dataSet = mutableListOf<LinearData>()

    var indexes = Pair(-1, -1)
    var indexOfMetric = -1
    BufferedReader(FileReader(file)).lines().forEach {
        if (indexes.first == -1) {
            val headerItems = it.split(delim)
            indexes = when (mode) {
                Mode.SCATTER -> Pair(headerItems.indexOf(ACTUAL), headerItems.indexOf(PREDICTED))
                Mode.LINE -> Pair(headerItems.indexOf(NR_EVENTS), headerItems.indexOf(SCORE))
            }

            indexOfMetric = if (Mode.SCATTER == mode) -1 else headerItems.indexOf(METRIC)
        } else {
            val items = it.split(delim)
            dataSet.add(
                    LinearData(
                            x = items[indexes.first].toFloat(),
                            y = items[indexes.second].toFloat(),
                            dataType = if (Mode.SCATTER == mode) "" else items[indexOfMetric]))
        }
    }
    return dataSet.sortedBy { it.dataType }
}

const val LABEL_INDEX = 0
const val VALUE_INDEX = 1

class BarChartData(val label: String, val value: Float)

fun getBarChartPayload(file: File): List<BarChartData> {
    val dataSet = mutableListOf<BarChartData>()

    var rows = BufferedReader(FileReader(file)).use { it.readLines() }
    rows = rows.subList(1, rows.size)

    rows.forEach {
        val items = it.split(delim)
        dataSet.add(BarChartData(items.get(LABEL_INDEX), items.get(VALUE_INDEX).toFloat()))
    }

    return dataSet
}