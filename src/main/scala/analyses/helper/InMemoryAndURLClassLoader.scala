package analyses.helper

import java.net.{URL, URLClassLoader}

class InMemoryAndURLClassLoader(
                                 private[this] var rawClasses: Map[String, Array[Byte]],
                                 parent: ClassLoader = ClassLoader.getSystemClassLoader,
                                 urls: Array[URL] = Array.empty
                               ) extends URLClassLoader(urls, parent) {

  /** @note Clients should call `loadClass`! Please, consult the documentation of
   *        `java.lang.ClassLoader` for further details!
   */
  @throws[ClassNotFoundException]
  override def findClass(name: String): Class[_] = {
    rawClasses.get(name) match {
      case Some(data) ⇒
        val clazz = defineClass(name, data, 0, data.length)
        rawClasses -= name
        clazz
      case None ⇒
        val loaded = findLoadedClass(name)
        if (loaded == null) {
          super.findClass(name)
        } else {
          loaded
        }
    }
  }
}
