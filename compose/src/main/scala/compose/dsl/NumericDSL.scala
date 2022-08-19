package compose.dsl

import compose.~>
import compose.Lambda.{constant, make}
import compose.dsl.NumericDSL.IsNumeric
import compose.execution.ExecutionPlan.NumericOperation
import zio.schema.Schema

trait NumericDSL[-A, +B] { self: A ~> B =>
  final def /[A1 <: A, B1 >: B](other: A1 ~> B1)(implicit num: IsNumeric[B1]): A1 ~> B1 =
    self divide other

  final def >[A1 <: A, B1 >: B](other: A1 ~> B1)(implicit num: IsNumeric[B1]): A1 ~> Boolean =
    self gt other

  final def >=[A1 <: A, B1 >: B](other: A1 ~> B1)(implicit num: IsNumeric[B1]): A1 ~> Boolean =
    self gte other

  final def <[A1 <: A, B1 >: B](other: A1 ~> B1)(implicit num: IsNumeric[B1]): A1 ~> Boolean =
    self lt other

  final def <=[A1 <: A, B1 >: B](other: A1 ~> B1)(implicit num: IsNumeric[B1]): A1 ~> Boolean =
    self lte other

  final def -[A1 <: A, B1 >: B](other: A1 ~> B1)(implicit num: IsNumeric[B1]): A1 ~> B1 =
    self + other.negate

  final def +[A1 <: A, B1 >: B](other: A1 ~> B1)(implicit num: IsNumeric[B1]): A1 ~> B1 =
    make[A1, B1](NumericOperation(NumericOperation.Add(self.compile, other.compile), num.kind))

  final def *[A1 <: A, B1 >: B](other: A1 ~> B1)(implicit num: IsNumeric[B1]): A1 ~> B1 =
    self multiply other

  final def dec[B1 >: B](implicit ev: IsNumeric[B1], s: Schema[B1]): A ~> B1 =
    self - constant(ev.one)

  final def divide[A1 <: A, B1 >: B](other: A1 ~> B1)(implicit num: IsNumeric[B1]): A1 ~> B1 =
    make[A1, B1](NumericOperation(NumericOperation.Divide(self.compile, other.compile), num.kind))

  final def gt[A1 <: A, B1 >: B](other: A1 ~> B1)(implicit num: IsNumeric[B1]): A1 ~> Boolean =
    make[A1, Boolean](NumericOperation(NumericOperation.GreaterThan(self.compile, other.compile), num.kind))

  final def gte[A1 <: A, B1 >: B](other: A1 ~> B1)(implicit num: IsNumeric[B1]): A1 ~> Boolean =
    make[A1, Boolean](NumericOperation(NumericOperation.GreaterThanEqualTo(self.compile, other.compile), num.kind))

  final def inc[B1 >: B](implicit ev: IsNumeric[B1], s: Schema[B1]): A ~> B1 =
    self + constant(ev.one)

  final def lt[A1 <: A, B1 >: B](other: A1 ~> B1)(implicit num: IsNumeric[B1]): A1 ~> Boolean =
    (self >= other).not

  final def lte[A1 <: A, B1 >: B](other: A1 ~> B1)(implicit num: IsNumeric[B1]): A1 ~> Boolean =
    (self gt other).not

  final def multiply[A1 <: A, B1 >: B](other: A1 ~> B1)(implicit num: IsNumeric[B1]): A1 ~> B1 =
    make[A1, B1](NumericOperation(NumericOperation.Multiply(self.compile, other.compile), num.kind))

  final def negate[B1 >: B](implicit num: IsNumeric[B1]): A ~> B1 = {
    make[A, B1](NumericOperation(NumericOperation.Negate(self.compile), num.kind))
  }
}

object NumericDSL {

  sealed trait IsNumeric[A] {
    def kind: NumericOperation.Kind

    def one: A

    def zero: A
  }

  implicit case object IsInt extends IsNumeric[Int] {
    override def kind: NumericOperation.Kind = NumericOperation.Kind.IntNumber

    override def one: Int = 1

    override def zero: Int = 0
  }

}
