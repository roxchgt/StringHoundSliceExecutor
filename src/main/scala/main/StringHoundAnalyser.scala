package main

import com.rabbitmq.client.{CancelCallback, Channel, Connection, ConnectionFactory, DeliverCallback, Delivery}
import org.slf4j.{Logger, LoggerFactory}

import java.nio.charset.StandardCharsets

object StringHoundAnalyser extends App {

  val logger: Logger = LoggerFactory.getLogger(StringHoundAnalyser.getClass)

  val slice_queue: String = "slice-queue"
  val result_queue: String = "result-queue"
  val factory: ConnectionFactory = new ConnectionFactory()
  factory.setHost("rabbitmq")
  val connection: Connection = factory.newConnection()
  val channel: Channel = connection.createChannel()
  channel.basicQos(1)

  channel.queueDeclare(slice_queue, false, false, false, null)
  logger.info("- waiting for incoming slices...")

  val callback: DeliverCallback = (tag: String, delivery: Delivery) => {
    logger.info(s"got message from {}", tag)
    val payload: String = new String(delivery.getBody, StandardCharsets.UTF_8)

    if (execute(payload)) {
      channel.basicAck(delivery.getEnvelope.getDeliveryTag, false)
      channel.basicPublish("", result_queue, delivery.getProperties, payload.concat(" DONE").getBytes())
    }
    else {
      logger.error(s"payload {} could not be processed", payload)
      channel.basicNack(delivery.getEnvelope.getDeliveryTag, false, false)
    }
  }

  val cancelCallback: CancelCallback = (tag: String) => {
    logger.error(s"An error was occurred on channel {}", tag)
  }

  channel.basicConsume(slice_queue, false, callback, cancelCallback)


  def execute(input: String): Boolean = {
    logger.info(s"... working on {}", input)
    if (input.length < 3) false else {
      if (input.length > 9) Thread.sleep(5000) else {
        Thread.sleep(2000)
      }
      true
    }
  }

}