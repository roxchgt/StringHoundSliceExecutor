package analyses.slicing

import analyses.helper.Utils.ScopeFunctions
import analyses.slicing.SliceExecutor.DUMMYSTATIC
import main.{Config => config}
import org.apache.commons.lang3.ClassUtils
import org.opalj.util.InMemoryAndURLClassLoader
import org.slf4j.LoggerFactory

import java.lang
import java.lang.reflect.Constructor
import java.net.URL
import scala.util.{Failure, Success, Try}

object SliceExecutor {
  private[slicing] val DUMMYSTATIC = "DUMMYSTATIC"
}

class SliceExtractExecutor(jarName: String) {
  private val logger = LoggerFactory.getLogger(classOf[SliceExtractExecutor])

  def getJarName: String = jarName
  private val urls: Array[URL] =
    Array.empty // yet to be fixed in the upstream version

  def execute(
      classFileExtract: ClassFileExtract,
      modifiedMethodExtract: MethodTemplateExtract,
      strippedClasses: Set[ClassFileExtract],
      mappedClasses: Map[String, Array[Byte]],
      attempt: Int
  ): Try[List[String]] = {
    Try {
      val classLoader = new InMemoryAndURLClassLoader(
        mappedClasses,
        this.getClass.getClassLoader,
        urls
      )

      strippedClasses
        .filter(c => c.fqn != classFileExtract.fqn && c.fqn != "slicing.StringLeaker")
        .foreach { rcf =>
          try {
            classLoader.loadClass(rcf.thisType)
          } catch {
            // Needed because sometimes it throws a NoClassDefFoundError which is not caught by the Try
            case th: Throwable => throw new Exception(th)
          }
        }

      val thread = new XCallThread(
        classLoader,
        classFileExtract.thisType,
        modifiedMethodExtract,
        attempt
      )

      runThread(thread)

      thread.results match {
        case Success(value)     => value
        case Failure(exception) => throw exception
      }
    }
  }

  private def runThread(thread: Thread): Unit = {
    thread.start()

    if (config.stopExecutionAfter <= 0) {
      thread.join()
    } else {
      thread.join(config.stopExecutionAfter)

      if (thread.isAlive) {
        try {
          classOf[Thread].getMethod("stop").invoke(thread)

          if (config.logSlicing) {
            logger.info(
              s"Forcefully killed thread ${thread.getName} after ${config.stopExecutionAfter.toDouble / 1000} seconds"
            )
          }
        } catch {
          case th: Throwable => logger.error(jarName, th)
        }

      }
    }
  }
}

private class XCallThread(
    val classLoader: ClassLoader,
    val targetClassName: String,
    val modifiedMethod: MethodTemplateExtract,
    attempt: Int
) extends Thread(s"Slice-Call-Thread $attempt") {
  private val logger = LoggerFactory.getLogger(classOf[XCallThread])

  var results: Try[List[String]] = _

  override def run(): Unit = {
    results = Try {
      try {
        call(classLoader, targetClassName, modifiedMethod)
      } catch {
        case error: Error  => throw new RuntimeException(error)
        case it: Throwable => throw it
      }
    }
  }

  private def call(
      classLoader: ClassLoader,
      targetClassName: String,
      modifiedMethod: MethodTemplateExtract
  ): List[String] = {
    val resultClass = classLoader.loadClass("slicing.StringLeaker")
    val targetClass = classLoader.loadClass(targetClassName)

    val zeroArgConstructor = getZeroArgsConstructor(targetClass)
    val argConstructors    = getArgConstructors(targetClass)
    val method             = getAlikeMethod(targetClass, modifiedMethod)

    val results = modifiedMethod.name match {
      case "<clinit>" =>
        List(callStaticInitializer(targetClass, resultClass))
      case "<init>" if zeroArgConstructor.isDefined =>
        List(callInitializer(zeroArgConstructor.get, resultClass))
      case _ if modifiedMethod.isStatic && method.isDefined =>
        List(callStaticMethod(method.get, resultClass))
      case _ if zeroArgConstructor.isDefined && method.isDefined =>
        List(
          callInstanceMethod(
            callConstructor(zeroArgConstructor.get),
            method.get,
            resultClass
          )
        )
      case _ if method.isDefined =>
        argConstructors.map { constructor =>
          var res: String = null

          try {
            res = callInstanceMethod(
              callConstructor(constructor),
              method.get,
              resultClass
            )
          } catch {
            case th: Throwable =>
              logger.error(
                s"calling $targetClassName#${modifiedMethod.name} with constructor ${constructor.getParameterTypes
                  .mkString("[", ", ", "]")}",
                th
              )
          }

          res
        }
      case _ => List()
    }

    if (config.logSlicing && config.debug) {
      logger.debug(s"Yielded results: ${results.mkString(", ")}")
    }

    results.filterNot(_ == null)
  }

  private def callStaticInitializer(
      targetClass: Class[_],
      resultClass: Class[_]
  ): String = {
    if (config.logSlicing && config.debug) {
      logger.debug(
        s"Executing static initializer for class ${targetClass.getName}"
      )
    }

    targetClass.getMethod(DUMMYSTATIC).invoke(null)

    getResult(resultClass)
  }

  private def callInitializer(
      constructor: Constructor[_],
      resultClass: Class[_]
  ): String = {
    if (config.logSlicing && config.debug) {
      logger.debug(
        s"Executing initializer for class ${constructor.getDeclaringClass.getName}"
      )
    }

    constructor.newInstance()

    getResult(resultClass)
  }

  private def callStaticMethod(
      method: lang.reflect.Method,
      resultClass: Class[_]
  ): String = {
    invoke(method, null, getParametersFor(method.getParameterTypes))

    getResult(resultClass)
  }

  private def callInstanceMethod(
      instance: Object,
      method: lang.reflect.Method,
      resultClass: Class[_]
  ): String = {
    invoke(method, instance, getParametersFor(method.getParameterTypes))

    getResult(resultClass)
  }

  private def invoke(
      method: lang.reflect.Method,
      instance: Object,
      parameters: Array[_ <: Object]
  ) = {
    if (config.logSlicing && config.debug) {
      logger.debug(
        s"Invoking method ${method.toString} with parameters ${parameters
          .mkString("[", ", ", "]")} on instance $instance"
      )
    }

    method.invoke(instance, parameters: _*)
  }

  private def getAlikeMethod(
      targetClass: Class[_],
      methodTemplate: MethodTemplateExtract
  ): Option[lang.reflect.Method] = {
    targetClass.getDeclaredMethods
      .find { method =>
        method.getName == methodTemplate.name && method.getParameterTypes
          .sameElements(methodTemplate.parameterTypes)
      }
      .also { opt =>
        opt.foreach(_.setAccessible(true))
      }
  }

  private def getZeroArgsConstructor(
      targetClass: Class[_]
  ): Option[Constructor[_]] = {
    targetClass.getDeclaredConstructors.find(_.getParameterCount == 0).also { opt =>
      opt.foreach(_.setAccessible(true))
    }
  }

  private def getArgConstructors(
      targetClass: Class[_]
  ): List[Constructor[_]] = {
    targetClass.getDeclaredConstructors
      .filter(_.getParameterCount > 0)
      .also { list =>
        list.foreach(_.setAccessible(true))
      }
      .toList
  }

  private def callConstructor(constructor: Constructor[_]): Object = {
    val parameters = getParametersFor(constructor.getParameterTypes)

    constructor.newInstance(parameters: _*).asInstanceOf[Object]
  }

  private def getParametersFor(
      parameterTypes: Array[Class[_]]
  ): Array[Object] = {
    parameterTypes.map(createInstanceOf).map(_.asInstanceOf[Object])
  }

  private def createInstanceOf(clazz: Class[_]): Any = if (clazz.isPrimitive) {
    createInstanceOfPrimitive(ClassUtils.primitiveToWrapper(clazz))
  } else {
    val const =
      clazz.getDeclaredConstructors.find(c => c.getParameterTypes.isEmpty && c.isAccessible)

    if (const.isDefined) {
      const.get.newInstance()
    } else {
      null
    }
  }

  private def createInstanceOfPrimitive(clazz: Class[_]): Any = clazz match {
    case w if w == classOf[Integer]           => Integer.valueOf(0)
    case w if w == classOf[java.lang.Boolean] => java.lang.Boolean.FALSE
    case w if w == classOf[java.lang.Long]    => java.lang.Long.valueOf(0L)
    case w if w == classOf[java.lang.Short] =>
      java.lang.Short.valueOf(0.asInstanceOf[Short])
    case w if w == classOf[java.lang.Byte] =>
      java.lang.Byte.valueOf(0.asInstanceOf[Byte])
    case w if w == classOf[java.lang.Character] =>
      java.lang.Character.valueOf('a')
    case w if w == classOf[java.lang.Float]  => java.lang.Float.valueOf(0.0f)
    case w if w == classOf[java.lang.Double] => java.lang.Double.valueOf(0.0)
  }

  private def getResult(resultClass: Class[_]): String =
    resultClass.getDeclaredField("result").get(null).asInstanceOf[String]
}
