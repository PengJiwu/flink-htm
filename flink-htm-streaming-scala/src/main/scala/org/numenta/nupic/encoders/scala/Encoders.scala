package org.numenta.nupic.encoders.scala

import org.numenta.nupic.encoders.scala.Encoders._
import org.joda.time.format.ISODateTimeFormat
import org.numenta.nupic.Parameters.KEY
import org.numenta.nupic.{encoders => jencoders}
import org.numenta.nupic.scala.Parameters

object MultiEncoder {
  def apply(encoders: jencoders.Encoder.Builder[_,_ <: jencoders.Encoder[_]]*): jencoders.MultiEncoder = {
    val multi = jencoders.MultiEncoder.builder().build()
    encoders.foreach {  builder =>
      val encoder = builder.build()
      multi.addEncoder(encoder.getName, encoder)
    }
    multi
  }
}

object DateEncoder {
  def apply(): jencoders.DateEncoder.Builder = {
    jencoders.DateEncoder.builder()
      .defaults
  }
}

object ScalarEncoder {
  def apply(): jencoders.ScalarEncoder.Builder = {
    jencoders.ScalarEncoder.builder().asInstanceOf[jencoders.ScalarEncoder.Builder]
      .defaults
  }
}

object AdaptiveScalarEncoder {
  def apply(): jencoders.AdaptiveScalarEncoder.Builder = {
    jencoders.AdaptiveScalarEncoder.adaptiveBuilder()
      .defaults
  }
}

object RandomDistributedScalarEncoder {
  def apply(): jencoders.RandomDistributedScalarEncoder.Builder = {
    jencoders.RandomDistributedScalarEncoder.builder().asInstanceOf[jencoders.RandomDistributedScalarEncoder.Builder]
      .defaults
  }
}


object CategoryEncoder {
  def apply():jencoders.CategoryEncoder.Builder = {
    jencoders.CategoryEncoder.builder().asInstanceOf[jencoders.CategoryEncoder.Builder]
      .defaults
  }
}

object Encoders {
  val params = Parameters.encoderDefaultParameters

  implicit class RichEncoder[T <: jencoders.Encoder.Builder[_, _]](builder: T) {
    def defaults: T = {
      builder.n(params.get(KEY.N).asInstanceOf[Int])
      builder.w(params.get(KEY.W).asInstanceOf[Int])
      builder.minVal(params.get(KEY.MIN_VAL).asInstanceOf[Double])
      builder.maxVal(params.get(KEY.MAX_VAL).asInstanceOf[Double])
      builder.radius(params.get(KEY.RADIUS).asInstanceOf[Double])
      builder.resolution(params.get(KEY.RESOLUTION).asInstanceOf[Double])
      builder.periodic(params.get(KEY.PERIODIC).asInstanceOf[Boolean])
      builder.clipInput(params.get(KEY.CLIP_INPUT).asInstanceOf[Boolean])
      builder.forced(params.get(KEY.FORCED).asInstanceOf[Boolean])
      builder
    }
  }
}
