package analyses.slicing

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

@SerialVersionUID(1L)
case class ClassFileExtract(fqn: String, thisType: String) extends Serializable

@SerialVersionUID(2L)
case class MethodTemplateExtract(name: String, parameterTypes: List[Class[_]], isStatic: Boolean)
    extends Serializable
