package com.ancientpoet.server.route

import com.ancientpoet.server.config.AppConfig
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.minio.MinioClient
import io.minio.PutObjectArgs
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.uploadRoute() {
    val config: AppConfig by inject()
    val minio = MinioClient.builder().endpoint(config.minioEndpoint).credentials(config.minioAccessKey, config.minioSecretKey).build()

    authenticate("auth-jwt") {
        post("/upload/image") {
            val bytes = call.receiveStream().readBytes()
            val filename = "${UUID.randomUUID()}.jpg"
            val bucket = config.minioBucket

            val found = minio.bucketExists(io.minio.BucketExistsArgs.builder().bucket(bucket).build())
            if (!found) minio.makeBucket(io.minio.MakeBucketArgs.builder().bucket(bucket).build())

            minio.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket).`object`(filename)
                    .stream(bytes.inputStream(), bytes.size.toLong(), -1)
                    .contentType("image/jpeg")
                    .build()
            )
            call.respond(HttpStatusCode.OK, mapOf("url" to "${config.minioEndpoint}/$bucket/$filename"))
        }
    }
}
