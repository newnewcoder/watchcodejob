package com.github.newnewcoder

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.linecorp.bot.client.LineMessagingServiceBuilder
import com.linecorp.bot.model.PushMessage
import com.linecorp.bot.model.message.TextMessage

import java.io.File
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger

private fun doSearch(boardName: String) {
    val BASE_ROOT = "https://www.ptt.cc"
    val BOARD = "/bbs/$boardName"
    val BASE_PATH = BASE_ROOT + BOARD

    val USER_AGENT = "User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36"

    val TITLE_PATTERN = "<a href=\"($BOARD.*)\">(.*)</a>".toRegex()

    val SEPARATOR = " ::: "

    val TEMP_FILE = "cj_tmp"

    // line
    val TOKEN: String = System.getenv("TOKEN")
    val ROOM_ID: String = System.getenv("ROOM_ID")

    FuelManager.instance.basePath = BASE_PATH
    FuelManager.instance.baseHeaders = mapOf("user-agent" to USER_AGENT)
    val (request, response, result) = "/index.html".httpGet().responseString()
    val (data, error) = result
    if (error == null) {
        System.out.println(Date().toString())
        System.out.println("==========$boardName==========")
        if (!File(TEMP_FILE).exists()) {
            File(TEMP_FILE).createNewFile()
        }
        val oldPosts = File(TEMP_FILE).readLines().map {
            it.split(SEPARATOR)[0] to it.split(SEPARATOR)[1]
        }.toMap()
        val postToSave = arrayListOf<String>()
        TITLE_PATTERN.findAll(data.toString()).forEach {
            val postRawData = it.value.replace(TITLE_PATTERN, "$1$SEPARATOR$2")
            val post = postRawData.split(SEPARATOR)
            if (!oldPosts.containsKey(post[0])) {
                postToSave.add(postRawData)
                sendMessage(TOKEN, ROOM_ID, BASE_ROOT + postRawData)
                System.out.println(BASE_ROOT + postRawData)
            }
        }
        postToSave.forEach {
            File(TEMP_FILE).appendText(it + System.getProperty("line.separator"))
        }
        System.out.println("==========$boardName==========")
    } else {
        System.err.println(error.toString())
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

//    Executors.newSingleThreadScheduledExecutor()
//            .scheduleAtFixedRate({
//                listOf("CodeJob", "soho").forEach {
//                    doSearch(it)
//                }
//            }, 0, 2, TimeUnit.MINUTES)

    listOf("CodeJob", "soho").forEach {
        doSearch(it)
    }
}