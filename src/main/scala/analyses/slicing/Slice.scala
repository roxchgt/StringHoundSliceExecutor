package analyses.slicing

import org.opalj.br.{ClassFile, Method, MethodTemplate, Type}

import java.io.Serializable
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

    def extractSlice(): SliceExtract = SliceExtract(
        new ClassFileExtract(classFile),
        new MethodTemplateExtract(modifiedMethod),
        strippedClasses.map(cf => new ClassFileExtract(cf)),
        mappedClasses.toList,
        attempt
    )
}

case class SliceExtract(classFileExtract: ClassFileExtract,
                        modifiedMethodExtract: MethodTemplateExtract,
                        strippedClassesExtract: Set[ClassFileExtract],
                        mappedClasses: List[(String, Array[Byte])],
                        attempt: Int
                       ) extends Serializable

case class ClassFileExtract(fqn: String, thisType: String) extends Serializable {
    def this(cf: ClassFile) = this(cf.fqn, cf.thisType.toJava)
}

case class MethodTemplateExtract(name: String, methodTypes: Seq[Class[_]], isStatic: Boolean) extends Serializable {
    def this(modMethod: MethodTemplate) = this(
        modMethod.name,
        //modMethod.parameterTypes.toList.map(t => t.toJavaClass),
        modMethod.parameterTypes.toList.map(_.toJavaClass),
        modMethod.isStatic)
}
