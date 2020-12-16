import com.ubirch.Service
import com.ubirch.controllers.{ InfoController, ResourcesController }
import javax.servlet.ServletContext
import org.scalatra.LifeCycle

/**
  * Represents the configuration of controllers
  */
class ScalatraBootstrap extends LifeCycle {

  lazy val infoController: InfoController = Service.get[InfoController]
  lazy val resourceController: ResourcesController = Service.get[ResourcesController]

  override def init(context: ServletContext) {

    context.setInitParameter("org.scalatra.cors.preflightMaxAge", "5")
    context.setInitParameter("org.scalatra.cors.allowCredentials", "false")

    context.mount(
      handler = infoController,
      urlPattern = "/",
      name = "Info"
    )
    context.mount(
      handler = resourceController,
      urlPattern = "/api-docs",
      name = "Resources"
    )
  }
}

