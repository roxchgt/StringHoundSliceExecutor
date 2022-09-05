package main

import com.rabbitmq.client.{Channel, Connection, ConnectionFactory}

object BrokerConnection {

  val slices_queue: String       = "slices-queue"
  val results_queue: String      = "results-queue"
  val factory: ConnectionFactory = new ConnectionFactory()
  val connection: Connection     = factory.newConnection()
  factory.setHost(config.brokerHost)
  val channel: Channel = connection.createChannel()
  private val config   = Config
  channel.basicQos(1)

  channel.queueDeclare(results_queue, false, false, false, null)
  channel.queueDeclare(slices_queue, false, false, false, null)

}
