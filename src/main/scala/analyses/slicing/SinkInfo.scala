package analyses.slicing

import org.opalj.br.ReferenceType

case class SinkInfo(sinkDeclaringClass: ReferenceType, sinkMethod: String, sinkPC: Int)
