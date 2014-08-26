package com.sksamuel.scrimage

/** Describes how to read Color from raw data.
  * The ColorModel may use several channels to encode the color,
  * and must provide a way to access to each of this channel.
  */
trait ColorModel {

  /** The data type that contains pixel information in this color model.
    * For example, if you were implementing an ARGB color model, you may chose to use ints
    * and pack the 4 components into an int using bit shifting, or you may choose to use 4 bytes.
    */
  @specialized(Byte)
  type ChannelType

  /** The number of channels used by this ColorModel
    */
  val n_channel: Int
  val maxChannelValue: Int

  def readChannel(offset: Int, pixel: Array[ChannelType]): Int

  def writeChannel(offset: Int, pixel: Array[ChannelType])(level: Int): Unit

  def readARGB(offset: Int, pixel: Array[ChannelType]): Int

  def writeARGB(offset: Int, pixel: Array[ChannelType])(argb: Int): Unit

  def readColor(offset: Int, pixel: Array[ChannelType]): Color

  def writeColor(offset: Int, pixel: Array[ChannelType])(color: Color): Unit

  /** Creates an array of ChannelType of the correct size for the given dim of raster */
  def newDataModel(width: Int, height: Int): Array[ChannelType]
}

/** A ColorModel that where channels value ranges between 0 and 255 and are store in one Byte*/
trait ByteColorModel extends ColorModel {

  final type ChannelType = Byte
  final val channelSize = 1
  final val maxChannelValue = 255

  @inline
  final def readChannel(off: Int, pixel: Array[ChannelType]) = pixel(off) & 0xff

  @inline
  final def writeChannel(off: Int, pixel: Array[ChannelType])(level: Int) =
    pixel(off) = level.toByte

  final def newDataModel(width: Int, height: Int) =
    Array.ofDim[Byte](width * height * n_channel)
}

trait ARGBColorModel extends ByteColorModel {
  final val n_channel = 4
  final def readARGB(off: Int, pixel: Array[ChannelType]) = (
    readChannel(off + 0, pixel) << 24 |
    readChannel(off + 1, pixel) << 16 |
    readChannel(off + 2, pixel) << 8 |
    readChannel(off + 3, pixel))

  final def writeARGB(off: Int, pixel: Array[ChannelType])(argb: Int) = {
    pixel(off) = (argb >> 24).toByte
    pixel(off + 1) = (argb >> 16).toByte
    pixel(off + 2) = (argb >> 8).toByte
    pixel(off + 3) = argb.toByte
  }
  final def readColor(off: Int, pixel: Array[ChannelType]) =
    Color(
      readChannel(off + 1, pixel),
      readChannel(off + 2, pixel),
      readChannel(off + 3, pixel),
      readChannel(off + 0, pixel))

  final def writeColor(off: Int, pixel: Array[ChannelType])(color: Color) = {
    val c = color.toRGB
    pixel(off) = c.alpha.toByte
    pixel(off + 1) = c.red.toByte
    pixel(off + 2) = c.green.toByte
    pixel(off + 3) = c.blue.toByte
  }
}

trait RGBColorModel extends ByteColorModel {
  final val n_channel = 3
  final def readARGB(off: Int, pixel: Array[ChannelType]) = (
    0xff000000 |
    readChannel(off + 0, pixel) << 16 |
    readChannel(off + 1, pixel) << 8 |
    readChannel(off + 2, pixel))

  final def writeARGB(off: Int, pixel: Array[ChannelType])(argb: Int) = {
    pixel(off + 0) = (argb >> 16).toByte
    pixel(off + 1) = (argb >> 8).toByte
    pixel(off + 2) = argb.toByte
  }
  final def readColor(off: Int, pixel: Array[ChannelType]) =
    Color(
      readChannel(off + 0, pixel),
      readChannel(off + 1, pixel),
      readChannel(off + 2, pixel))

  def writeColor(off: Int, pixel: Array[ChannelType])(color: Color) = {
    val c = color.toRGB
    pixel(off + 0) = c.red.toByte
    pixel(off + 1) = c.green.toByte
    pixel(off + 2) = c.blue.toByte
  }
}

/** A ColorModel for grayscale colors. */
trait GrayColorModel extends ByteColorModel {
  final val n_channel = 1
  final def readARGB(off: Int, pixel: Array[ChannelType]) = readChannel(off, pixel) * 0x00010101 | 0xff000000
  final def writeARGB(off: Int, pixel: Array[ChannelType])(argb: Int) = {
    writeColor(off, pixel)(Color(argb))
  }
  final def readColor(off: Int, pixel: Array[ChannelType]) = Color(readARGB(off, pixel))
  final def writeColor(off: Int, pixel: Array[ChannelType])(color: Color) = {
    val c = color.toRGB
    pixel(off) = (c.red * 0.2989f + c.green * 0.5870f + c.blue * 0.1140f).toByte
  }
}

/** A ColorModel for grayscale colors with alpha. */
trait GrayAlphaColorModel extends ByteColorModel {
  final val n_channel = 2
  final def readARGB(off: Int, pixel: Array[ChannelType]) =
    readChannel(off, pixel) * 0x00010101 | readChannel(off, pixel) << 24
  final def writeARGB(off: Int, pixel: Array[ChannelType])(argb: Int) = {
    writeColor(off, pixel)(Color(argb))
  }
  final def readColor(off: Int, pixel: Array[ChannelType]) = Color(readARGB(off, pixel))
  final def writeColor(off: Int, pixel: Array[ChannelType])(color: Color) = {
    val c = color.toRGB
    pixel(off) = (c.red * 0.2989f + c.green * 0.5870f + c.blue * 0.1140f).toByte
    pixel(off + 1) = c.alpha.toByte
  }
}

/** A ColorModel that stores only one of the RGB channel. */
class RGBLayerColorModel(val channel: Int) extends ByteColorModel {
  final val n_channel = 1
  final def readARGB(off: Int, pixel: Array[ChannelType]) = (
    pixel(off) << (16 - channel * 8) | 0xff000000
  )
  final def writeARGB(off: Int, pixel: Array[ChannelType])(argb: Int) = {
    pixel(off) = (argb >> (16 - channel * 8)).toByte
  }

  final def readColor(off: Int, pixel: Array[ChannelType]) = Color(readARGB(off, pixel))

  final def writeColor(off: Int, pixel: Array[ChannelType])(color: Color) =
    writeARGB(off, pixel)(color.toRGB.argb)
}

