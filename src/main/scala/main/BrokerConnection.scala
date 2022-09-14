package main

import com.rabbitmq.client.{Channel, Connection, ConnectionFactory}

import java.util

object BrokerConnection {

  private val config = Config

  val slices_queue: String  = "slices-queue"
  val results_queue: String = "results-queue"

  val factory: ConnectionFactory = new ConnectionFactory
  factory.setHost(config.brokerHost)

  val connection: Connection = factory.newConnection
  val channel: Channel       = connection.createChannel
  val args                   = new util.HashMap[String, Object]
  args.put("x-message-ttl", Integer.valueOf(30000))

  channel.basicQos(1)

  channel.queueDeclare(results_queue, false, false, false, args)
  channel.queueDeclare(slices_queue, false, false, false, null)

}
