package com.ancientpoet.server.route

import com.ancientpoet.server.config.AppConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject
import java.io.File
import java.util.UUID

fun Route.uploadRoute() {
    val appConfig: AppConfig by inject()

    authenticate("auth-jwt") {
        post("/upload/image") {
            val multipart = call.receiveMultipart()
            var url: String? = null

            multipart.forEachPart { part ->
                if (part is io.ktor.server.request.PartData.FileItem) {
                    val ext = part.originalFileName?.substringAfterLast('.', "jpg") ?: "jpg"
                    val filename = "${UUID.randomUUID()}.$ext"
                    val uploadDir = File("uploads")
                    uploadDir.mkdirs()
                    val file = File(uploadDir, filename)
                    part.streamProvider().use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    url = "/static/$filename"
                }
                part.dispose()
            }

            if (url != null) {
                call.respond(HttpStatusCode.OK, mapOf("url" to url))
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "未找到上传文件"))
            }
        }
    }
}
