package maia.ml.learner.standard.hoeffdingtree

import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/*
 * Copyright 2007 University of Waikato.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */



/**
 * Class implementing some serialize utility methods.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
object SerializeUtils {
    @Throws(IOException::class)
    fun writeToFile(file: File?, obj: Serializable?) {
        val out = ObjectOutputStream(
            GZIPOutputStream(
                BufferedOutputStream(FileOutputStream(file))
            )
        )
        out.writeObject(obj)
        out.flush()
        out.close()
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    fun readFromFile(file: File?): Any {
        val `in` = ObjectInputStream(
            GZIPInputStream(
                BufferedInputStream(FileInputStream(file))
            )
        )
        val obj = `in`.readObject()
        `in`.close()
        return obj
    }

    @Throws(Exception::class)
    fun copyObject(obj: Serializable?): Any {
        val baoStream = ByteArrayOutputStream()
        val out = ObjectOutputStream(
            BufferedOutputStream(baoStream)
        )
        out.writeObject(obj)
        out.flush()
        out.close()
        val byteArray = baoStream.toByteArray()
        val `in` = ObjectInputStream(
            BufferedInputStream(
                ByteArrayInputStream(byteArray)
            )
        )
        val copy = `in`.readObject()
        `in`.close()
        return copy
    }

    @Throws(Exception::class)
    fun measureObjectByteSize(obj: Serializable?): Int {
        val bcoStream = ByteCountingOutputStream()
        val out = ObjectOutputStream(
            BufferedOutputStream(bcoStream)
        )
        out.writeObject(obj)
        out.flush()
        out.close()
        return bcoStream.numBytesWritten
    }

    class ByteCountingOutputStream : OutputStream() {
        var numBytesWritten = 0
            protected set

        @Throws(IOException::class)
        override fun write(b: Int) {
            numBytesWritten++
        }

        @Throws(IOException::class)
        override fun write(b: ByteArray, off: Int, len: Int) {
            numBytesWritten += len
        }

        @Throws(IOException::class)
        override fun write(b: ByteArray) {
            numBytesWritten += b.size
        }
    }
}