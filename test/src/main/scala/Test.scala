
import shapeless._

object Test {
  val i: Int = 1 :: "a" :: 1.1 :: HNil

  type ISB = Int :+: String :+: Boolean :+: CNil

  val x: ISB = "a"

  case class User(name: String, age: Int, isAdmin: Boolean)

  val lgen = LabelledGeneric[User]

  lgen.from(User("name", 30, false))

}
