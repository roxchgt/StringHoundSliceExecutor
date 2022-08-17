package analyses.slicing

import main.{Config => config}
import org.apache.commons.lang3.ClassUtils
import org.opalj.br.{ClassFile, MethodTemplate}
import org.opalj.util.InMemoryClassLoader
import org.slf4j.LoggerFactory
import Utils.ScopeFunctions

import java.lang
import java.lang.reflect.Constructor
import java.net.URL
import scala.util.{Failure, Success, Try}

class SliceExecutor(private val slicingAnalysis: SlicingAnalysis) {
    private val jarName = slicingAnalysis.jarName
    private val statistics = slicingAnalysis.statistics

    private val logger = LoggerFactory.getLogger(classOf[SliceExecutor])

    private val urls: Array[URL] = /* if (config.isAndroid) { */ Array.empty
    /*} else {
        // TODO (Svenja, 13/05/2022): File structure changed, additional libs for java might need to be loaded from somewhere else

        // new File(/home/roxch/bs/worker-node/src/main/scala/sliceslicingAnalysis.workingDir).listFiles(it => it.getName.endsWith(".jar")).map(_.toURI.toURL)
        // workingDir was the parent dir of the input file

    }*/

    def execute(classFile: ClassFile, modifiedMethod: MethodTemplate, strippedClasses: Set[ClassFile], mappedClasses: Map[String, Array[Byte]], attempt: Int): Try[List[String]] = {
        Try {
            //TODO (Rosh, 01.08.22): InMemoryAndUrlClassLoader not in official OPAL packages
            val classLoader = new InMemoryClassLoader(mappedClasses, this.getClass.getClassLoader)

            strippedClasses.filter(c => c.fqn != classFile.fqn && c.fqn != "slicing.StringLeaker").foreach { rcf =>
                try {
                    // TODO (Svenja, 04/02/2022): Why does this sometimes throw a NoClassDefFoundError?
                    classLoader.loadClass(rcf.thisType.toJava)
                } catch {
                    // Needed because sometimes it throws a NoClassDefFoundError which is not caught by the Try
                    case th: Throwable => throw new Exception(th)
                }
            }

            val thread = new CallThread(classLoader, classFile.thisType.toJava, modifiedMethod, attempt)

            runThread(thread)

            thread.results match {
                case Success(value) => value
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
                        logger.info(s"Forcefully killed thread ${thread.getName} after ${config.stopExecutionAfter.toDouble / 1000} seconds")
                    }
                } catch {
                    case th: Throwable => logger.error(jarName, th)
                }

                statistics.timeouts.incrementAndGet()
            }
        }
    }

    private class CallThread(val classLoader: ClassLoader, val targetClassName: String, val modifiedMethod: MethodTemplate, attempt: Int) extends Thread(s"Slice-Call-Thread $attempt") {
        private val logger = LoggerFactory.getLogger(classOf[CallThread])

        var results: Try[List[String]] = _

        override def run(): Unit = {
            results = Try {
                try {
                    call(classLoader, targetClassName, modifiedMethod)
                } catch {
                    case error: Error => throw new RuntimeException(error)
                    case it: Throwable => throw it
                }
            }
        }

        private def call(classLoader: ClassLoader, targetClassName: String, modifiedMethod: MethodTemplate): List[String] = {
            val resultClass = classLoader.loadClass("slicing.StringLeaker")
            val targetClass = classLoader.loadClass(targetClassName)

            val zeroArgConstructor = getZeroArgsConstructor(targetClass)
            val argConstructors = getArgConstructors(targetClass)
            val method = getAlikeMethod(targetClass, modifiedMethod)

            val results = modifiedMethod.name match {
                case "<clinit>" => List(callStaticInitializer(
                    targetClass,
                    resultClass
                ))
                case "<init>" if zeroArgConstructor.isDefined => List(callInitializer(
                    zeroArgConstructor.get,
                    resultClass
                ))
                case _ if modifiedMethod.isStatic && method.isDefined => List(callStaticMethod(
                    method.get,
                    resultClass
                ))
                case _ if zeroArgConstructor.isDefined && method.isDefined => List(callInstanceMethod(
                    callConstructor(zeroArgConstructor.get),
                    method.get,
                    resultClass
                ))
                case _ if method.isDefined => argConstructors.map { constructor =>
                    var res: String = null

                    try {
                        res = callInstanceMethod(
                            callConstructor(constructor),
                            method.get,
                            resultClass
                        )
                    } catch {
                        case th: Throwable => logger.error(s"calling $targetClassName#${modifiedMethod.name} ${modifiedMethod.descriptor} with constructor ${constructor.getParameterTypes.mkString("[", ", ", "]")}", th)
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

        private def callStaticInitializer(targetClass: Class[_], resultClass: Class[_]): String = {
            if (config.logSlicing && config.debug) {
                logger.debug(s"Executing static initializer for class ${targetClass.getName}")
            }

            /*targetClass.getMethod(DUMMYSTATIC).invoke(null)

            getResult(resultClass)*$

             */
            "kokokok"
        }

        private def callInitializer(constructor: Constructor[_], resultClass: Class[_]): String = {
            if (config.logSlicing && config.debug) {
                logger.debug(s"Executing initializer for class ${constructor.getDeclaringClass.getName}")
            }

            constructor.newInstance()

            getResult(resultClass)
        }

        private def callStaticMethod(method: lang.reflect.Method, resultClass: Class[_]): String = {
            invoke(method, null, getParametersFor(method.getParameterTypes))

            getResult(resultClass)
        }

        private def callInstanceMethod(instance: Object, method: lang.reflect.Method, resultClass: Class[_]): String = {
            invoke(method, instance, getParametersFor(method.getParameterTypes))

            getResult(resultClass)
        }

        private def invoke(method: lang.reflect.Method, instance: Object, parameters: Array[_ <: Object]) = {
            if (config.logSlicing && config.debug) {
                logger.debug(s"Invoking method ${method.toString} with parameters ${parameters.mkString("[", ", ", "]")} on instance $instance")
            }

            method.invoke(instance, parameters: _*)
        }

        private def getAlikeMethod(targetClass: Class[_], methodTemplate: MethodTemplate): Option[lang.reflect.Method] = {
            targetClass.getDeclaredMethods.find { method =>
                method.getName == methodTemplate.name && method.getParameterTypes.sameElements(methodTemplate.parameterTypes.map(_.toJavaClass))
            }.also { opt =>
                opt.foreach(_.setAccessible(true))
            }
        }

        private def getZeroArgsConstructor(targetClass: Class[_]): Option[Constructor[_]] = {
            targetClass.getDeclaredConstructors.find(_.getParameterCount == 0).also { opt =>
                opt.foreach(_.setAccessible(true))
            }
        }

        private def getArgConstructors(targetClass: Class[_]): List[Constructor[_]] = {
            targetClass.getDeclaredConstructors.filter(_.getParameterCount > 0).also { list =>
                list.foreach(_.setAccessible(true))
            }.toList
        }

        private def getAllConstructors(targetClass: Class[_]): List[Constructor[_]] = {
            targetClass.getDeclaredConstructors.also { list =>
                list.foreach(_.setAccessible(true))
            }.toList
        }

        private def callConstructor(constructor: Constructor[_]): Object = {
            val parameters = getParametersFor(constructor.getParameterTypes)

            constructor.newInstance(parameters: _*).asInstanceOf[Object]
        }

        private def getParametersFor(parameterTypes: Array[Class[_]]): Array[Object] = {
            parameterTypes.map(createInstanceOf).map(_.asInstanceOf[Object])
        }

        private def createInstanceOf(clazz: Class[_]): Any = if (clazz.isPrimitive) {
            createInstanceOfPrimitive(ClassUtils.primitiveToWrapper(clazz))
        } else {
            val const = clazz.getDeclaredConstructors.find(c => c.getParameterTypes.isEmpty && c.canAccess())

            if (const.isDefined) {
                const.get.newInstance()
            } else {
                null
            }
        }

        private def createInstanceOfPrimitive(clazz: Class[_]): Any = clazz match {
            case w if w == classOf[Integer] => Integer.valueOf(0)
            case w if w == classOf[java.lang.Boolean] => java.lang.Boolean.FALSE
            case w if w == classOf[java.lang.Long] => java.lang.Long.valueOf(0L)
            case w if w == classOf[java.lang.Short] => java.lang.Short.valueOf(0.asInstanceOf[Short])
            case w if w == classOf[java.lang.Byte] => java.lang.Byte.valueOf(0.asInstanceOf[Byte])
            case w if w == classOf[java.lang.Character] => java.lang.Character.valueOf('a')
            case w if w == classOf[java.lang.Float] => java.lang.Float.valueOf(0.0f)
            case w if w == classOf[java.lang.Double] => java.lang.Double.valueOf(0.0)
        }

        // TODO (Svenja, 22/02/2022): Try to recursively recreate all parameters
        private def createInstanceOfClass(clazz: Class[_]): Any = {
            if (clazz == classOf[String]) {
                return ""
            }

            val constructors = getAllConstructors(clazz)

            val obj = constructors.map { constr =>
                var res: Any = null

                try {
                    res = constr.newInstance(getParametersFor(constr.getParameterTypes))
                } catch {
                    case ex: Throwable =>
                }

                res
            }.filterNot(_ == null).collectFirst({ case x => x })

            if (obj.isDefined) {
                obj.get
            } else {
                null
            }

            //        val zeroArgConstructor = getZeroArgsConstructor(clazz)
            //
            //        if (zeroArgConstructor.isDefined) {
            //            zeroArgConstructor.get.newInstance()
            //        } else {
            //            null
            //        }
        }

        private def getResult(resultClass: Class[_]): String = resultClass.getDeclaredField("result").get(null).asInstanceOf[String]
    }
}