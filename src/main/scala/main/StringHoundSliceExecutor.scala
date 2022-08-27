package main

import analyses.slicing.SliceExtract
import com.rabbitmq.client._
import org.apache.commons.lang3.SerializationUtils
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try


object StringHoundSliceExecutor extends App {

  val logger: Logger = LoggerFactory.getLogger(StringHoundSliceExecutor.getClass)
  val config = Config

  val slices_queue: String = "slices-queue"
  val results_queue: String = "results-queue"

  val factory: ConnectionFactory = new ConnectionFactory()
  factory.setHost(config.brokerHost)
  val connection: Connection = factory.newConnection()
  val channel: Channel = connection.createChannel()
  channel.basicQos(1)

  channel.queueDeclare(results_queue, false, false, false,null)

  val deliverCallback: DeliverCallback = (tag: String, delivery: Delivery) => {
    logger.info(s"– received slice of jar: {}", delivery.getProperties.getAppId)
    val deliveryTag: Long = delivery.getEnvelope.getDeliveryTag
    try {
      val sliceExtract: SliceExtract = SerializationUtils.deserialize[SliceExtract](delivery.getBody)
      val result: Try[List[String]] = sliceExtract.execute()
      val serializedResult = SerializationUtils.serialize(result)
      channel.basicPublish("", results_queue, delivery.getProperties, serializedResult)
      channel.basicAck(deliveryTag, false)
    } catch {
      case x: Exception =>
        logger.error(x.getMessage)
        channel.basicReject(deliveryTag, false)
    }
  }

  channel.queueDeclare(slices_queue, false, false, false, null)

  logger.info("- waiting for incoming slices...")

  channel.basicConsume(slices_queue, false, deliverCallback, (tag: String) => {})
}
