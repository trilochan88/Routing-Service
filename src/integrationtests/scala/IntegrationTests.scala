import org.scalatest.flatspec.AnyFlatSpec

/** */
class IntegrationTests extends AnyFlatSpec with DockerComposeTestSuite {
  "Routing Service" should "retries for slow connection for times as per config and service should respond with service unavailable for post request" in {}
  it should "if health of server is not healthy then on post request" in {}
  it should "if all given servers are healthy then request should round-robin the response" in {}
}
