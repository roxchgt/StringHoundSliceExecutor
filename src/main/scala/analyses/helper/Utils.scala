package analyses.helper

object Utils {

  implicit class ScopeFunctions[T](val obj: T) extends AnyVal {
    /**
     * Calls the specified function `block` with `this` value as its argument and returns `this`.
     */
    @inline
    def also(block: T => Unit): T = {
      block(obj)
      obj
    }

    /**
     * Calls the specified function `block` with `this` value as its argument and returns the result.
     */
    @inline
    def let[A](block: T => A): A = block(obj)
  }
}
