package com.pirouette.nyagent.infrastructure.linux

import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.GZIPInputStream

/**
 * Minimal tar.gz extractor used to unpack the bundled proot + Alpine rootfs
 * blobs from app resources into app-private storage. Handles regular files,
 * directories, and symlinks with the classic USTAR header layout, including
 * GNU long-name entries.
 */
class GzipTarExtractor {

    /** Reports each extracted/created entry by path as the archive is unpacked. */
    var onEntry: ((String) -> Unit)? = null

    fun extract(stream: InputStream, destination: File) {
        if (!destination.exists() && !destination.mkdirs()) {
            throw IOException("Cannot create destination: " + destination.absolutePath)
        }

        val input = GZIPInputStream(BufferedInputStream(stream))
        val header = ByteArray(512)
        val buffer = ByteArray(512)
        var pendingLongName: String? = null

        while (true) {
            val read = readFully(input, header, 0, 512)
            if (read < 512 || isAllZero(header)) break

            val name = readString(header, 0, 100)
            val prefix = readString(header, 345, 100)
            val rawSize = readOctal(header, 124, 12)
            val typeFlag = header[156].toInt() and 0xff
            val linkName = readString(header, 157, 100)
            val mode = readOctal(header, 100, 8)

            if (typeFlag == 0x4c) { // GNU long name entry
                val longName = ByteArray(rawSize.toInt())
                readFully(input, longName, 0, rawSize.toInt())
                pendingLongName = String(longName, StandardCharsets.UTF_8).trim()
                skipPadding(rawSize, input)
                continue
            }

            val entryPath = pendingLongName ?: if (prefix.isNotEmpty()) "$prefix/$name" else name

            when (typeFlag) {
                0x35 -> { // directory
                    File(destination, entryPath).mkdirs()
                    skipPadding(rawSize, input)
                }
                0x32 -> { // symlink
                    val link = File(destination, entryPath)
                    link.parentFile?.mkdirs()
                    if (link.exists()) {
                        link.delete()
                    }
                    Files.createSymbolicLink(link.toPath(), Paths.get(linkName))
                    skipPadding(rawSize, input)
                }
                0x67, 0x78 -> { // PAX global / extended headers (0x67 = g, 0x78 = x)
                    skipPadding(rawSize, input)
                    pendingLongName = null
                    continue
                }
                else -> {
                    val file = File(destination, entryPath)
                    file.parentFile?.mkdirs()
                    writeFile(input, file, rawSize, buffer)
                    file.setExecutable((mode and 0x40L) != 0L)
                    file.setReadable(true)
                    file.setWritable((mode and 0x80L) != 0L)
                    skipPadding(rawSize, input)
                }
            }

            onEntry?.invoke(entryPath)
            pendingLongName = null
        }
        input.close()
    }

    private fun writeFile(input: InputStream, file: File, size: Long, buffer: ByteArray) {
        val output = FileOutputStream(file)
        try {
            var remaining = size
            while (remaining > 0) {
                val chunk = Math.min(remaining, 512).toInt()
                val read = readFully(input, buffer, 0, chunk)
                if (read <= 0) break
                output.write(buffer, 0, read)
                remaining -= read
            }
        } finally {
            output.close()
        }
    }

    private fun skipPadding(size: Long, input: InputStream) {
        val padding = ((size + 511) / 512) * 512 - size
        var remaining = padding
        val skipBuffer = ByteArray(512)
        while (remaining > 0) {
            val chunk = Math.min(remaining, 512).toInt()
            val read = input.read(skipBuffer, 0, chunk)
            if (read == -1) break
            remaining -= read
        }
    }

    private fun isAllZero(bytes: ByteArray): Boolean {
        var index = 0
        while (index < bytes.size) {
            if (bytes[index].toInt() != 0) return false
            index++
        }
        return true
    }

    private fun readOctal(bytes: ByteArray, offset: Int, length: Int): Long {
        val text = String(bytes, offset, length, StandardCharsets.UTF_8).replace("\u0000", "").trim()
        if (text.isEmpty()) return 0
        return try {
            java.lang.Long.parseLong(text, 8)
        } catch (e: Exception) {
            java.lang.Long.parseLong(text)
        }
    }

    private fun readString(bytes: ByteArray, offset: Int, length: Int): String {
        val sb = StringBuilder()
        var index = offset
        while (index < offset + length) {
            val c = bytes[index].toInt() and 0xff
            if (c == 0) break
            sb.append(c.toChar())
            index++
        }
        return sb.toString().trim()
    }

    private fun readFully(input: InputStream, buffer: ByteArray, offset: Int, length: Int): Int {
        var total = 0
        while (total < length) {
            val read = input.read(buffer, offset + total, length - total)
            if (read == -1) break
            total += read
        }
        return total
    }
}
