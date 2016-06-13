package boopickle

import java.nio.{ByteBuffer, ShortBuffer}
import java.nio.charset.StandardCharsets

import scala.scalajs.js
import scala.scalajs.js.typedarray.TypedArrayBufferOps._
import scala.scalajs.js.typedarray._

/**
  * Facade for native JS engine provided TextDecoder
  */
@js.native
class TextDecoder extends js.Object {
  def decode(data: Int8Array): String = js.native
}

/**
  * Facade for native JS engine provided TextEncoder
  */
@js.native
class TextEncoder extends js.Object {
  def encode(str: String): Uint8Array = js.native
}

object StringCodec extends StringCodecFuncs {
  private lazy val utf8decoder: (Int8Array) => String = {
    val td = new TextDecoder
    // use native TextDecoder
    (data: Int8Array) => td.decode(data)
  }

  private lazy val utf8encoder: (String) => Int8Array = {
    val te = new TextEncoder
    // use native TextEncoder
    (str: String) => new Int8Array(te.encode(str))
  }

  override def decodeUTF8(len: Int, buf: ByteBuffer): String = {
    if (buf.isDirect && !js.isUndefined(js.Dynamic.global.TextDecoder)) {
      // get the underlying Int8Array
      val ta = buf.typedArray()
      val s = utf8decoder(ta.subarray(buf.position, buf.position + len))
      buf.position(buf.position + len)
      s
    } else {
      val bb = buf.slice()
      bb.limit(len)
      val s = StandardCharsets.UTF_8.decode(bb).toString
      buf.position(buf.position + len)
      s
    }
  }

  override def encodeUTF8(s: String): ByteBuffer = {
    if (js.isUndefined(js.Dynamic.global.TextEncoder)) {
      StandardCharsets.UTF_8.encode(s)
    } else {
      TypedArrayBuffer.wrap(utf8encoder(s))
    }
  }

  override def decodeUTF16(len: Int, buf: ByteBuffer): String = {
    if (buf.isDirect) {
      val ta = new Uint16Array(buf.typedArray().buffer, buf.position + buf.typedArray().byteOffset, len/2)
      buf.position(buf.position + len)
      js.Dynamic.global.String.fromCharCode.applyDynamic("apply")(null, ta).asInstanceOf[String]
      //new String(ta.toArray) // alt implementation
    } else {
      val bb = buf.slice()
      bb.limit(len)
      val s = StandardCharsets.UTF_16LE.decode(bb).toString
      buf.position(buf.position + len)
      s
    }
  }

  override def encodeUTF16(s: String): ByteBuffer = {
    val ta = new Uint16Array(s.length)
    var i = 0
    while (i < s.length) {
      ta(i) = s.charAt(i).toShort
      i += 1
    }
    TypedArrayBuffer.wrap(new Int8Array(ta.buffer))
  }
}
