import scala.concurrent._, scala.util._
import cats._, cats.data._, cats.implicits._
implicit val cte = ExecutionContext.fromExecutor(_.run())

case class User(id: Int,
                username: String,
                email: String = "test@test.com",
                firstName: String = "firstname",
                lastName: String = "lastname",
                supervisorId: Int = 1)

trait UserRepository {
  def get(id: Int): User
  def find(username: String): User
}
object UserRepository extends UserRepository {
  override def get(id: Int): User = User(id, java.util.UUID.randomUUID().toString)
  override def find(username: String): User = User(1, username)
}
trait UserService {
  def getUser(id: Int): Reader[UserRepository, User] = Reader(_.get(id))
  def findUser(username: String): Reader[UserRepository, User] = Reader(_.find(username))
}
object UserService extends UserService {

  def userInfo(username: String): Reader[UserRepository, Map[String, String]] =
    for {
      user <- findUser(username)
      boss <- getUser(user.supervisorId)
    } yield Map(
      "fullName" -> s"${user.firstName} ${user.lastName}",
      "email" -> s"${user.email}",
      "boss" -> s"${boss.firstName} ${boss.lastName}"
    )
}

println(UserService.userInfo("mert")(UserRepository))

