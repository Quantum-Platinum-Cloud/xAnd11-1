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

package org.monksanctum.xand11.extension

import org.monksanctum.xand11.Client
import org.monksanctum.xand11.comm.PacketReader
import org.monksanctum.xand11.comm.PacketWriter
import org.monksanctum.xand11.comm.XProtoReader
import org.monksanctum.xand11.comm.XProtoWriter
import org.monksanctum.xand11.comm.XSerializable

/**
 * Based off from Big Requests Extension
 * from http://www.x.org/releases/X11R7.7/doc/bigreqsproto/bigreq.html
 * with the following copyright notice.
 *
 * Copyright © 1993, 1994 X Consortium
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the “Software”),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * X CONSORTIUM BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of the X Consortium shall not be
 * used in advertising or otherwise to promote the sale, use or other dealings in
 * this Software without prior written authorization from the X Consortium.
 *
 * X Window System is a trademark of The Open Group.
 */
class BigRequests : Extension("BIG-REQUESTS") {

    override fun handleRequest(client: Client, reader: PacketReader, writer: PacketWriter) {
        client.clientListener.setBigRequestEnabled()
        writer.writeCard32(BIG_REQUEST_MAX_SIZE)
        writer.writePadding(20)
    }

    companion object {

        val BIG_REQUEST_MAX_SIZE = 0x00080000
    }
}
