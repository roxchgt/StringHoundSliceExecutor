package main

import com.rabbitmq.client.{Channel, Connection, ConnectionFactory}

object BrokerConnection {

  private val config = Config

  val slices_queue: String = "slices-queue"
  val results_queue: String = "results-queue"

  val factory: ConnectionFactory = new ConnectionFactory()
  factory.setHost(config.brokerHost)
  val connection: Connection = factory.newConnection()

  val channel: Channel = connection.createChannel()
  channel.basicQos(1)

  channel.queueDeclare(results_queue, false, false, false, null)
  channel.queueDeclare(slices_queue, false, false, false, null)

}