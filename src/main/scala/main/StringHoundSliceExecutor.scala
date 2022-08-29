package main

import analyses.slicing.SliceExtract
import com.rabbitmq.client._
import main.BrokerConnection.{channel, results_queue, slices_queue}
import org.apache.commons.lang3.SerializationUtils
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try


object StringHoundSliceExecutor extends App {

  val logger: Logger = LoggerFactory.getLogger(StringHoundSliceExecutor.getClass)
  val brokerConnection = BrokerConnection


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

  logger.info("- waiting for incoming slices...")
  channel.basicConsume(slices_queue, false, deliverCallback, (tag: String) => {})
}
