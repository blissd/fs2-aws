package fs2
package aws

import java.io._

import cats.effect.{Effect, IO}
import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.model.{AmazonS3Exception, GetObjectRequest, S3ObjectInputStream}
import fs2.aws.internal.Internal.S3Client
import org.apache.http.client.methods.HttpRequestBase

import scala.io.Source

package object utils {

  val s3TestClient: S3Client[IO] = new S3Client[IO] {
    override def getObjectContent(getObjectRequest: GetObjectRequest)(implicit e: Effect[IO]) : IO[S3ObjectInputStream] = getObjectRequest match {
      case goe: GetObjectRequest => {
        val is : InputStream = {
          val fileContent: Array[Byte] =
            try {
              Source.fromResource(goe.getKey).mkString.getBytes
            }
            catch {
              case _ : FileNotFoundException => throw new AmazonS3Exception("File not found")
              case e: Throwable => throw e
            }
          goe.getRange match {
            case Array(x, y) =>

              if (x >= fileContent.length) throw new AmazonS3Exception("Invalid range")
              else if (y > fileContent.length) new ByteArrayInputStream(fileContent.slice(x.toInt, fileContent.length))
              else new ByteArrayInputStream(fileContent.slice(x.toInt, y.toInt))
          }
        }

        IO {
          Thread.sleep(500)  // simulate a call to S3
          new S3ObjectInputStream(is, new HttpRequestBase { def getMethod = "" })
        }
      }
      case _ => throw new SdkClientException("Invalid GetObjectRequest")
    }
  }
}
