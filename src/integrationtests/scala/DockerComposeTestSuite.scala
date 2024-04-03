import org.scalatest.{BeforeAndAfterAll, Suite}

import scala.sys.process.*
trait DockerComposeTestSuite extends BeforeAndAfterAll {
  this: Suite ⇒
  override def beforeAll(): Unit = {
    super.beforeAll()
    s"docker-compose up -d".!
  }

  override def afterAll(): Unit = {
    s"docker-compose down --remove orphans".!
    super.afterAll()
  }
}
