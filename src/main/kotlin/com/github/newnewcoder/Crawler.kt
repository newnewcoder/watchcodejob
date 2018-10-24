package com.github.newnewcoder

import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.linecorp.bot.client.LineMessagingServiceBuilder
import com.linecorp.bot.model.PushMessage
import com.linecorp.bot.model.message.TextMessage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


private fun doScratchPtt(baseRoot: String, boardName: String): String {
    val userAgent = "User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36"
    val basePath =  "$baseRoot$boardName"
    FuelManager.instance.basePath = basePath
    FuelManager.instance.baseHeaders = mapOf("user-agent" to userAgent)
    val (_, _, result) = "/index.html".httpGet().responseString()
    val (data, error) = result
    return when (error == null) {
        true -> {
            data.orEmpty()
        }
        else -> {
            ""
        }
    }

}

private fun doPersist(file: Path, data: String) {
    if (Files.notExists(file)) {
        Files.createFile(file)
    }
    file.toFile().appendText(data + System.getProperty("line.separator"))
}

private fun loadPersistedData(file: Path): List<String> {
    return when (Files.exists(file)) {
        true -> {
            file.toFile().readLines()
        }
        else -> {
            emptyList()
        }
    }
}

private fun sendMessage(token: String, roomId: String, txt: String) {
    LineMessagingServiceBuilder
            .create(token)
            .build()
            .pushMessage(PushMessage(roomId, TextMessage(txt)))
            .execute()
}

fun main(args: Array<out String>) {
    val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    root.level = Level.ERROR

    val ptt = "https://www.ptt.cc"
    val tmp = Paths.get("cj_tmp.txt")
    val separator = " ::: "
    val token = System.getenv("TOKEN")
    val roomId = System.getenv("ROOM_ID")

    val oldData = loadPersistedData(tmp).map {
        it.split(separator)[0] to it.split(separator)[1]
    }.toMap()
    listOf("CodeJob", "soho").map{"/bbs/$it"}.forEach { board ->
        val pattern = "<a href=\"($board/M.*html)\">(.*)</a>".toRegex()
        val result = doScratchPtt(ptt, board)
        //println(result)
        pattern.findAll(result)
                .map { it.value.replace(pattern, "$1$separator$2").replace("<.*?>".toRegex(), "") }
                .filter {!oldData.containsKey(it.split(separator)[0]) }
                .forEach {
                    sendMessage(token, roomId, ptt + it)
                    doPersist(tmp, it)
                    println("""|==========$board==========
                               |$it
                               |==========$board==========""".trimMargin())
                }
    }

}
