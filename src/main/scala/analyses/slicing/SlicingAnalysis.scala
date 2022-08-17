package analyses.slicing

import org.opalj.br.analyses.Project

import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

class Statistics {
    //TODO no need here
    val genericErrors = new AtomicInteger
    val slicesCreated = new AtomicInteger
    val slicesEvaluated = new AtomicInteger
    val executions = new AtomicInteger
    val verifyErrors = new AtomicInteger
    val timeouts = new AtomicInteger
    val methodCalls = new AtomicInteger
    val successful = new AtomicInteger
    val invocationTargetError = new AtomicInteger
    val parameterErrors = new AtomicInteger
    val usesParams = new AtomicInteger
    val noClassDef = new AtomicInteger
    val otherSource = new AtomicInteger
    val nullPointerError = new AtomicInteger
}

class SlicingAnalysis(val project: Project[URL], val jarName: String, val startTimeMillis: Long) {

    private val sliceExecutor = new SliceExecutor(this)

    var constantStrings = Set.empty[String]
    var stringUsages = List.empty[String]

    val statistics = new Statistics

    var threadIds: Set[Long] = Thread.getAllStackTraces.keySet().toArray().map(f => f.asInstanceOf[Thread].getId).toSet

}

