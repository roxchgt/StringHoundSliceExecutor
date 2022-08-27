package main

import com.typesafe.config._

object Config {
  private val conf = ConfigFactory.load()

  val brokerHost:String = conf.getString("broker-host")
  val isAndroid:Boolean = conf.getBoolean("is-android")
  val stopExecutionAfter: Int = conf.getInt("stop-execution-after")
  val logSlicing: Boolean = conf.getBoolean("log-slicing")
  val debug: Boolean =  conf.getBoolean("debug")
}