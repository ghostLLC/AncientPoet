package com.ancientpoet.server.route

import com.ancientpoet.server.config.AppConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.minio.MinioClient
import io.minio.PutObjectArgs
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.uploadRoute() {
    val appConfig: AppConfig by inject()

    val minioClient = MinioClient.builder()
        .endpoint(appConfig.minioEndpoint)
        .credentials(appConfig.minioAccessKey, appConfig.minioSecretKey)
        .build()

    authenticate("auth-jwt") {
        post("/upload/image") {
            val multipart = call.receiveMultipart()
            var url: String? = null

            multipart.forEachPart { part ->
                if (part is io.ktor.server.request.PartData.FileItem) {
                    val ext = part.originalFileName?.substringAfterLast('.', "jpg") ?: "jpg"
                    val filename = "${UUID.randomUUID()}.$ext"
                    val contentType = when (ext) {
                        "png" -> "image/png"
                        "gif" -> "image/gif"
                        "webp" -> "image/webp"
                        else -> "image/jpeg"
                    }

                    val bucketName = appConfig.minioBucket
                    // Ensure bucket exists
                    val found = minioClient.bucketExists(
                        io.minio.BucketExistsArgs.builder().bucket(bucketName).build()
                    )
                    if (!found) {
                        minioClient.makeBucket(
                            io.minio.MakeBucketArgs.builder().bucket(bucketName).build()
                        )
                    }

                    part.streamProvider().use { input ->
                        minioClient.putObject(
                            PutObjectArgs.builder()
                                .bucket(bucketName)
                                .`object`(filename)
                                .stream(input, -1, 10485760) // 10MB max
                                .contentType(contentType)
                                .build()
                        )
                    }

                    url = "${appConfig.minioEndpoint}/$bucketName/$filename"
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
