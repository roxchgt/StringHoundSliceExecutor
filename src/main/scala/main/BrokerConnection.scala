package main

import com.rabbitmq.client.{Channel, Connection, ConnectionFactory}

import java.util

object BrokerConnection {

  private val config = Config

  val slices_queue: String = "slices-queue"
  val results_queue: String = "results-queue"

  val factory: ConnectionFactory = new ConnectionFactory()
  factory.setHost(config.brokerHost)
  val connection: Connection = factory.newConnection()

  val channel: Channel = connection.createChannel()
  channel.basicQos(1)

  val arguments: java.util.Map[String, Object] = util.Map.of[String, Object](
    "x-message-ttl",
    java.lang.Integer.valueOf(config.stopExecutionAfter + 3000) // Message Time-To-Live on Queues
  )

  channel.queueDeclare(results_queue, false, false, false, arguments)
  channel.queueDeclare(slices_queue, false, false, false, arguments)

}