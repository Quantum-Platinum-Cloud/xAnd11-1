// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.monksanctum.xand11.fonts

import android.graphics.Rect
import android.util.Log

import org.monksanctum.xand11.Client
import org.monksanctum.xand11.Dispatcher
import org.monksanctum.xand11.comm.Event
import org.monksanctum.xand11.comm.PacketReader
import org.monksanctum.xand11.comm.PacketWriter
import org.monksanctum.xand11.comm.Request
import org.monksanctum.xand11.errors.GContextError
import org.monksanctum.xand11.errors.XError
import org.monksanctum.xand11.fonts.Font.CharInfo
import org.monksanctum.xand11.fonts.Font.FontProperty
import org.monksanctum.xand11.graphics.GraphicsManager

import org.monksanctum.xand11.fonts.FontManager.Companion.DEBUG

class FontProtocol(private val mFontManager: FontManager, private val mGraphicsManager: GraphicsManager) : Dispatcher.PacketHandler {

    override val opCodes: ByteArray
        get() = HANDLED_OPS

    @Throws(XError::class)
    override fun handleRequest(client: Client, reader: PacketReader, writer: PacketWriter) {
        when (reader.majorOpCode) {
            Request.OPEN_FONT -> handleOpenFont(reader)
            Request.CLOSE_FONT -> handleCloseFont(reader)
            Request.QUERY_FONT -> handleQueryFont(reader, writer)
            Request.LIST_FONTS -> handleListFonts(reader, writer)
            Request.LIST_FONTS_WITH_INFO -> handleListFontsWithInfo(client, reader, writer)
            Request.QUERY_TEXT_EXTENTS -> handleQueryTextExtents(reader, writer)
            Request.GET_FONT_PATH -> handleGetFontPath(reader, writer)
        }
    }

    private fun handleGetFontPath(reader: PacketReader, writer: PacketWriter) {
        writer.writeCard16(0) // 0 Font paths
        writer.writePadding(22)
    }

    private fun handleListFontsWithInfo(client: Client, reader: PacketReader, writer: PacketWriter) {
        val max = reader.readCard16()
        val n = reader.readCard16()
        val pattern = reader.readPaddedString(n)
        val fonts = mFontManager.getFontsMatching(pattern, max)
        val N = fonts.size

        for (i in 0 until N) {
            val pWriter = PacketWriter(client.clientListener.writer)
            val font = fonts[i]
            val name = font.toString()
            pWriter.minorOpCode = name.length.toByte()
            writeFont(pWriter, font, N - i)
            pWriter.writePaddedString(name)
            pWriter.copyToReader().use { r -> logInfo("FontProtocol", r) }
            client.clientListener.sendPacket(Event.REPLY, pWriter)
        }

        writer.writePadding(52)
    }

    private fun handleListFonts(reader: PacketReader, writer: PacketWriter) {
        val max = reader.readCard16()
        val n = reader.readCard16()
        val pattern = reader.readPaddedString(n)
        val fonts = mFontManager.getFontsMatching(pattern, max)
        val N = fonts.size
        writer.writeCard16(N)
        writer.writePadding(22)
        val builder = StringBuilder()
        for (i in 0 until N) {
            val fontName = fonts[i].toString()
            builder.append(fontName.length.toByte().toChar())
            builder.append(fontName)
        }
        writer.writePaddedString(builder.toString())
    }

    private fun handleOpenFont(reader: PacketReader) {
        val fid = reader.readCard32()
        val length = reader.readCard16()
        reader.readPadding(2)
        val name = reader.readPaddedString(length)
        mFontManager.openFont(fid, name)
    }

    private fun handleCloseFont(reader: PacketReader) {
        val fid = reader.readCard32()
        mFontManager.closeFont(fid)
    }

    private fun handleQueryFont(reader: PacketReader, writer: PacketWriter) {
        val fid = reader.readCard32()
        val font = mFontManager.getFont(fid)
        if (font != null) {
            writeFont(writer, font, font.chars.size)
            writeChars(writer, font)
            writer.copyToReader().use { r -> logInfo("FontManager", r) }
        } else {
            Log.d("FontManager", "Sending empty font " + Integer.toHexString(fid))
            writer.writePadding(28)
        }
    }

    @Throws(GContextError::class)
    private fun handleQueryTextExtents(reader: PacketReader, writer: PacketWriter) {
        val odd = reader.minorOpCode.toInt() != 0
        val fid = reader.readCard32()
        var font: Font? = mFontManager.getFont(fid)
        if (font == null) {
            font = mFontManager.getFont(mGraphicsManager.getGc(fid).font)
        }
        val length = reader.remaining / 2 - if (odd) 1 else 0
        val s = reader.readString16(length)
        val bounds = Rect()
        val width = font!!.paint.measureText(s).toInt()
        font.paint.getTextBounds(s, 0, s.length, bounds)

        writer.writeCard16(font.maxBounds.ascent)
        writer.writeCard16(font.maxBounds.descent)
        writer.writeCard16((-bounds.top).toShort().toInt())
        writer.writeCard16(bounds.bottom.toShort().toInt())
        writer.writeCard32(width)
        writer.writeCard32(bounds.left)
        writer.writeCard32(bounds.right)
        writer.writePadding(4)
    }

    private fun writeFont(writer: PacketWriter, font: Font, sizeField: Int) {
        writeChar(writer, font.minBounds)
        writer.writePadding(4)
        writeChar(writer, font.maxBounds)
        writer.writePadding(4)

        writer.writeCard16(font.minCharOrByte2.toInt())
        writer.writeCard16(font.maxCharOrByte2.toInt())
        writer.writeCard16(font.defaultChar.toInt())
        val size = font.mFontProperties.size
        writer.writeCard16(size)

        writer.writeByte(if (font.isRtl) Font.RIGHT_TO_LEFT else Font.LEFT_TO_RIGHT)
        writer.writeByte(font.minByte1)
        writer.writeByte(font.maxByte1)
        writer.writeByte((if (font.allCharsExist) 1 else 0).toByte())

        writer.writeCard16(font.fontAscent)
        writer.writeCard16(font.fontDescent)
        writer.writeCard32(sizeField)
        for (i in 0 until size) {
            writeProperty(writer, font.mFontProperties[i])
        }
    }

    private fun writeChars(writer: PacketWriter, font: Font) {
        val chars = font.chars
        val N = chars.size
        for (i in 0 until N) {
            writeChar(writer, chars[i])
        }
    }

    private fun writeProperty(writer: PacketWriter, property: FontProperty) {
        writer.writeCard32(property.name)
        writer.writeCard32(property.value)
    }

    private fun writeChar(writer: PacketWriter, charInfo: Font.CharInfo) {
        writer.writeCard16(charInfo.leftSideBearing)
        writer.writeCard16(charInfo.rightSideBearing)
        writer.writeCard16(charInfo.characterWidth)
        writer.writeCard16(charInfo.ascent)
        writer.writeCard16(charInfo.descent)
        writer.writeCard16(charInfo.attributes)
    }

    companion object {

        private val HANDLED_OPS = byteArrayOf(Request.OPEN_FONT, Request.QUERY_FONT, Request.CLOSE_FONT, Request.LIST_FONTS, Request.LIST_FONTS_WITH_INFO, Request.QUERY_TEXT_EXTENTS, Request.GET_FONT_PATH)

        fun logInfo(tag: String, r: PacketReader) {
            if (!DEBUG) return
            Log.d(tag, "minBounds")
            logChar(tag, r)
            Log.d(tag, "")
            Log.d(tag, "maxBounds")
            logChar(tag, r)
            Log.d(tag, "")

            Log.d(tag, "minChar=" + r.readCard16() + " maxChar=" + r.readCard16())
            Log.d(tag, "defaultChar=" + r.readCard16())
            val numProps = r.readCard16()

            Log.d(tag, "")
            Log.d(tag, "isRtl:" + (r.readByte() == Font.RIGHT_TO_LEFT))
            Log.d(tag, "minByte=" + r.readByte() + " maxByte=" + r.readByte())
            Log.d(tag, "allChars:" + (r.readByte().toInt() != 0))
            Log.d(tag, "ascent=" + r.readCard16() + " descent=" + r.readCard16())
            Log.d(tag, "")
            Log.d(tag, "Remaining hint: " + r.readCard32())
            for (i in 0 until numProps) {
                Log.d(tag, "Prop: " + r.readCard32() + " " + r.readCard32())
            }
            Log.d(tag, "Name: " + r.readPaddedString(r.minorOpCode.toInt()))
        }

        private fun logChar(tag: String, r: PacketReader) {
            Log.d(tag, "leftSideBearing=" + r.readCard16()
                    + " rightSideBearing=" + r.readCard16())
            Log.d(tag, "width=" + r.readCard16())
            Log.d(tag, "ascent=" + r.readCard16() + " descent=" + r.readCard16())
            Log.d(tag, "attributes=" + r.readCard16())
            r.readPadding(4)
        }
    }
}
