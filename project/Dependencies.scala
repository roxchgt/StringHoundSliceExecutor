/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

import sbt._

/**
 * Manages the library dependencies of the subprojects of OPAL.
 *
 * @author Simon Leischnig
 */
object Dependencies {

  val br = Seq(scalaparsercombinators, scalaxml)

  def common(scalaVersion: String) = Seq(reflect(scalaVersion), scalaxml, playjson, ficus, fastutil)

  import library._

  object version {
    val scalaxml = "1.1.0"
    val playjson = "2.6.9"
    val ficus = "1.4.3"
    val scalaparsercombinators = "1.1.0"
    val fastutil = "8.1.1"
  }

  object library {

    val scalaxml = "org.scala-lang.modules" %% "scala-xml" % version.scalaxml
    val playjson = "com.typesafe.play" %% "play-json" % version.playjson
    val ficus = "com.iheart" %% "ficus" % version.ficus
    val scalaparsercombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % version.scalaparsercombinators
    // --- developer tools dependencies
    val fastutil = "it.unimi.dsi" % "fastutil" % version.fastutil withSources() withJavadoc()

    // --- general dependencies
    def reflect(scalaVersion: String) = "org.scala-lang" % "scala-reflect" % scalaVersion
  }

}
