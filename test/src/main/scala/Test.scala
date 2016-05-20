
import shapeless._

object Test {
  val i: Int = 1 :: "a" :: 1.1 :: HNil

  type ISB = Int :+: String :+: Boolean :+: CNil

  val x: ISB = "a"

  trait Formatter[T] {
    type Out
  }

  object Formatter {
    type Aux[T, U] = Formatter[T] {
      type Out = U
    }
  }

  implicitly[Formatter.Aux[Int :+: String :+: Boolean :+: CNil, String]]

  @annotation.implicitNotFound("Please provide an implicit instace of Aux2 for ${T}")
  trait Aux2[T]

  implicitly[Aux2[Int :+: String :+: Boolean :+: CNil]]

  case class User(name: String, age: Int, isAdmin: Boolean)

  val lgen = LabelledGeneric[User]

  lgen.from(User("name", 30, false))

}

