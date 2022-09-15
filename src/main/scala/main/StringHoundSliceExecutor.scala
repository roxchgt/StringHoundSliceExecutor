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

  val deliveryCallback: DeliverCallback = (tag: String, delivery: Delivery) => {
    val deliveryTag: Long = delivery.getEnvelope.getDeliveryTag
    val prop: AMQP.BasicProperties = delivery.getProperties
    val jarName: String = prop.getAppId
    if (!delivery.getEnvelope.isRedeliver) {
      if (prop.getType == "last") {
        Thread.sleep(2000)
        channel.basicPublish("", results_queue, prop, null)
        channel.basicAck(deliveryTag, false)
      } else {
        try {
          val sliceExtract: SliceExtract = SerializationUtils.deserialize(delivery.getBody)

          val result = sliceExtract.executeWith(getExecutor(jarName))

          val serializedResult = SerializationUtils.serialize(result)

          channel.basicPublish("", results_queue, prop, serializedResult)
          channel.basicAck(deliveryTag, false)

        } catch {
          case x: Exception =>
            logger.error(x.getMessage)
            channel.basicReject(deliveryTag, false)
        }
      }
    } else {
      channel.basicReject(deliveryTag, false)
    }
  }

  channel.basicConsume(
    slices_queue,
    false,
    deliveryCallback,
    (tag: String) => {
      logger.info(s"Consumer {} cancelled.", tag)
    }
  )

  logger.info("waiting for incoming slice extracts ...")
}
