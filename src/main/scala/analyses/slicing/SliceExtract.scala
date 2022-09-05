package analyses.slicing

import org.opalj.br.{ClassFile, MethodTemplate}

import java.io.Serializable
import scala.util.Try

@SerialVersionUID(123L)
case class SliceExtract(
                         classFileExtract: ClassFileExtract,
                         modifiedMethodExtract: MethodTemplateExtract,
                         strippedClassesExtract: Set[ClassFileExtract],
                         mappedClasses: Map[String, Array[Byte]],
                         attempt: Int
                       ) {
  def executeWith(sliceExtractExecutor: SliceExtractExecutor): Try[List[String]] =
    sliceExtractExecutor.execute(
      classFileExtract,
      modifiedMethodExtract,
      strippedClassesExtract,
      mappedClasses,
      attempt
    )
}

case class ClassFileExtract(fqn: String, thisType: String) extends Serializable {
  def this(cf: ClassFile) = this(cf.fqn, cf.thisType.toJava)
}

case class MethodTemplateExtract(name: String, parameterTypes: List[Class[_]], isStatic: Boolean)
  extends Serializable {
  def this(modMethod: MethodTemplate) =
    this(modMethod.name, modMethod.parameterTypes.toList.map(_.toJavaClass), modMethod.isStatic)
}
