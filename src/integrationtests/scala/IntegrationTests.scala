import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.*
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.settings.ConnectionPoolSettings
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Futures.timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.not.be
import org.scalatest.matchers.should.Matchers.{should, shouldBe, shouldEqual}
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class IntegrationTests
    extends AnyFlatSpec
    with BeforeAndAfterAll
    with ScalaFutures {

  implicit val system: ActorSystem = ActorSystem("IntegrationSystem")

  override def afterAll(): Unit = {
    system.terminate()
  }

  "Routing Service" should "respond with 200 Ok for a health check" in {
    val healthCheckUri =
      Uri("http://localhost:8080").withPath(Uri.Path("/health"))
    val httpPoolSettings = ConnectionPoolSettings(system)
      .withMaxConnections(3)
      .withMaxRetries(3)
      .withMaxOpenRequests(5)
    val response = Http().singleRequest(
      HttpRequest(uri = healthCheckUri),
      settings = httpPoolSettings
    )
    val result = Await.result(response, 5.second)
    result.status shouldEqual StatusCodes.OK
  }
  "Routing Service" should "retries for slow connection for times as per config " +
    "and service should respond with service unavailable for post request" in {
      val slowEndpoint = Uri("http://localhost:8080").withPath(
        Uri.Path("/ext/update-points-delay")
      )
      val testHeaders = List(RawHeader("Client-Type", "Delay"))
      val httpPoolSettings = ConnectionPoolSettings(system)
        .withMaxConnections(100)
        .withMaxRetries(3)
        .withMaxOpenRequests(100)
      val jsonStr =
        """
          |{
          |	"game":"Mobile",
          |	"gamerID": "GYUTDTE",
          |	"points":20
          |}
          |""".stripMargin

      val jsonEntity = HttpEntity(ContentTypes.`application/json`, jsonStr)

      val requests = (1 to 100).map { _ =>
        Http().singleRequest(
          HttpRequest(
            method = HttpMethods.POST,
            uri = slowEndpoint,
            headers = testHeaders,
            entity = jsonEntity
          ),
          settings = httpPoolSettings
        )
      }
      val aggregatedResponse = Future.sequence(requests)
      whenReady(aggregatedResponse, timeout(Span(30, Seconds))) { responses =>
        responses.foreach { response =>
          println(
            s"Response ${response.status} body: ${response.entity.dataBytes.toString}"
          )
          response.status should (be(StatusCodes.GatewayTimeout) or be(
            StatusCodes.ServiceUnavailable
          ))

        }
      }
    }
  it should "if health of server is not healthy then on post request" in {
    val jsonStr =
      """
        |{
        |	"game":"Mobile Legends",
        |	"gamerID": "GYUTDTE",
        |	"points":20
        |}
        |""".stripMargin
    val jsonEntity = HttpEntity(ContentTypes.`application/json`, jsonStr)
    val normalEndpoint =
      Uri("http://localhost:8080").withPath(Uri.Path("/ext/update-points"))
    val httpPoolSettings = ConnectionPoolSettings(system)
      .withMaxConnections(100)
      .withMaxRetries(3)
      .withMaxOpenRequests(100)

    val requests = (1 to 10).map { _ =>
      Http().singleRequest(
        HttpRequest(
          method = HttpMethods.POST,
          uri = normalEndpoint,
          entity = jsonEntity
        ),
        settings = httpPoolSettings
      )
    }
    val aggregatedResponse = Future.sequence(requests)
    whenReady(aggregatedResponse, timeout(Span(30, Seconds))) { responses =>
      responses.foreach { response =>
        println(
          s"Response ${response.status} body: ${response.entity.dataBytes.toString}"
        )
        response.status shouldBe StatusCodes.ServiceUnavailable

      }
    }
  }
  it should "if all given servers are healthy then request should round-robin the response" in {
    val jsonStr =
      """
        |{
        |	"game":"Mobile Legends",
        |	"gamerID": "GYUTDTE",
        |	"points":20
        |}
        |""".stripMargin
    val jsonEntity = HttpEntity(ContentTypes.`application/json`, jsonStr)
    val normalEndpoint =
      Uri("http://localhost:8080").withPath(Uri.Path("/ext/update-points"))
    val httpPoolSettings = ConnectionPoolSettings(system)
      .withMaxConnections(100)
      .withMaxRetries(3)
      .withMaxOpenRequests(100)

    val requests = (1 to 100).map { _ =>
      Http().singleRequest(
        HttpRequest(
          method = HttpMethods.POST,
          uri = normalEndpoint,
          entity = jsonEntity
        ),
        settings = httpPoolSettings
      )
    }
    val aggregatedResponse = Future.sequence(requests)
    whenReady(aggregatedResponse, timeout(Span(30, Seconds))) { responses =>
      responses.foreach { response =>
        println(
          s"Response ${response.status} body: ${response.entity.dataBytes.toString}"
        )
        response.status shouldBe StatusCodes.OK

      }
    }
  }
}
