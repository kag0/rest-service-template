package thepackage

import akka.http.scaladsl.marshalling.{PredefinedToEntityMarshallers, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest}
import akka.http.scaladsl.unmarshalling.{
  FromEntityUnmarshaller,
  FromRequestUnmarshaller,
  PredefinedFromEntityUnmarshallers,
  Unmarshaller
}
import play.api.libs.json._

import scala.concurrent.Future

package object util {

  implicit val jsonMarshaller: ToEntityMarshaller[JsValue] = PredefinedToEntityMarshallers
    .byteArrayMarshaller(ContentTypes.`application/json`)
    .compose[JsValue](Json.toBytes)

  implicit def writesEntityMarshaller[A](implicit writes: Writes[A]): ToEntityMarshaller[A] =
    jsonMarshaller.compose[A](writes.writes)

  implicit def readsUnmarshaller[A](implicit reads: Reads[A]) =
    Unmarshaller[JsValue, A](_ => js =>
      reads.reads(js) match {
        case JsSuccess(value, _) => Future.successful(value)
        case error: JsError      => Future.failed(JsResult.Exception(error))
      })

  implicit def readsEntityUnmarshaller[A](implicit unm: Unmarshaller[JsValue, A]): FromEntityUnmarshaller[A] =
    jsonUnmarshaller.andThen(unm)

  implicit def requestToEntUnm[A](implicit unm: FromEntityUnmarshaller[A]): FromRequestUnmarshaller[A] =
    Unmarshaller.strict[HttpRequest, HttpEntity](_.entity).andThen(unm)

  implicit val jsonUnmarshaller: FromEntityUnmarshaller[JsValue] =
    PredefinedFromEntityUnmarshallers.byteArrayUnmarshaller
      .map(Json.parse)
      .forContentTypes(ContentTypes.`application/json`)
}
