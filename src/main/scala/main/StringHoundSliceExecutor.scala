package main

import analyses.slicing.{SliceExtract, SliceExtractExecutor}
import com.rabbitmq.client._
import main.BrokerConnection.{channel, results_queue, slices_queue}
import org.apache.commons.lang3.SerializationUtils
import org.slf4j.{Logger, LoggerFactory}

object StringHoundSliceExecutor extends App {

  val logger: Logger = LoggerFactory.getLogger(StringHoundSliceExecutor.getClass)
  val brokerConnection = BrokerConnection
  var sliceExtractExecutor: Option[SliceExtractExecutor] = None

  private def getExecutor(jarName: String): SliceExtractExecutor = {
    if (!sliceExtractExecutor.exists(executor => executor.getJarName.equals(jarName))) {
      sliceExtractExecutor = Option[SliceExtractExecutor](new SliceExtractExecutor(jarName))
    }
    sliceExtractExecutor.get
  }

  logger.info("waiting for incoming slice extracts ...")
  channel.basicConsume(
    slices_queue,
    false,
    (tag: String, delivery: Delivery) => {
      val deliveryTag: Long = delivery.getEnvelope.getDeliveryTag

      val jarName = delivery.getProperties.getAppId
      try {
        val sliceExtract: SliceExtract = SerializationUtils.deserialize(delivery.getBody)

        val result = sliceExtract.executeWith(getExecutor(jarName))

        val serializedResult = SerializationUtils.serialize(result)

        channel.basicPublish("", results_queue, delivery.getProperties, serializedResult)
        channel.basicAck(deliveryTag, false)

      } catch {
        case x: Exception =>
          logger.error(x.getMessage)
          channel.basicReject(deliveryTag, false)
      }
    },
    (tag: String) => {
      logger.info(s"Consumer {} cancelled.", tag)
    }
  )
}
