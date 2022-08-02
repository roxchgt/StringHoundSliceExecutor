package slice

import org.opalj.br.{ClassFile, Method, MethodTemplate}

import scala.util.Try

case class Slice(classFile: ClassFile,
                 modifiedMethod: MethodTemplate,
                 strippedClasses: Set[ClassFile],
                 mappedClasses: Map[String, Array[Byte]],
                 method: Method,
                 sinkInfo: SinkInfo,
                 encryptedString: Option[String],
                 newClass: ClassFile,
                 attempt: Int) {

    def executeWith(sliceExecutor: SliceExecutor): Try[List[String]] = {
        sliceExecutor.execute(classFile, modifiedMethod, strippedClasses, mappedClasses, attempt)
    }
}
