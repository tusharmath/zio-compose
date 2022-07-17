package compose

import zio.prelude.NonEmptyList
import zio.schema.Schema

sealed trait Lambda[-A, +B] { self =>
  final def >>>[C](other: Lambda[B, C]): Lambda[A, C] = self pipe other

  final def <<<[X](other: Lambda[X, A]): Lambda[X, B] = self compose other

  final def ++[A1 <: A, B1 >: B, B2](
    other: Lambda[A1, B2],
  )(implicit
    b1: Schema[B1],
    b2: Schema[B2],
  ): A1 ~> (B1, B2) =
    (self: A1 ~> B1) zip other

  final def add[A1 <: A, B1 >: B](other: A1 ~> B1)(implicit
    num: Numeric.IsNumeric[B1],
    schema: Schema[Numeric.IsNumeric[B1]],
  ): A1 ~> B1 =
    Lambda.NumericOperation(Numeric.Operation.Add, self, other, num, schema)

  final def compose[X](other: Lambda[X, A]): Lambda[X, B] =
    Lambda.Pipe(other, self)

  final def equals[A1 <: A, B1 >: B](other: A1 ~> B1)(implicit
    schema: Schema[B1],
  ): A1 ~> Boolean =
    Lambda.Equals(self, other, schema)

  final def pipe[C](other: Lambda[B, C]): Lambda[A, C] =
    Lambda.Pipe(self, other)

  final def zip[A1 <: A, B1 >: B, B2](other: Lambda[A1, B2])(implicit
    b1: Schema[B1],
    b2: Schema[B2],
  ): A1 ~> (B1, B2) = Lambda.Combine(self, other, b1, b2)

  private[compose] final def compile: ExecutionPlan =
    ExecutionPlan.fromLambda(self)
}

object Lambda {

  def constant[B](a: B)(implicit schema: Schema[B]): Any ~> B =
    Constant(a, schema)

  def fromMap[A, B](
    source: Map[A, B],
  )(implicit input: Schema[A], output: Schema[B]): Lambda[A, B] =
    FromMap(input, source, output)

  def identity[A]: Lambda[A, A] = Identity[A]()

  def ifElse[A, B](f: A ~> Boolean)(isTrue: A ~> B, isFalse: A ~> B): A ~> B =
    IfElse(f, isTrue, isFalse)

  final case class Equals[A, B](left: A ~> B, right: A ~> B, schema: Schema[B]) extends Lambda[A, Boolean]

  final case class FromMap[A, B](input: Schema[A], source: Map[A, B], output: Schema[B]) extends Lambda[A, B]

  final case class Constant[B](b: B, schema: Schema[B]) extends Lambda[Any, B]

  final case class Identity[A]() extends Lambda[A, A]

  final case class Pipe[A, B, C](f: Lambda[A, B], g: Lambda[B, C]) extends Lambda[A, C]

  final case class Select[A, B](input: Schema[A], path: NonEmptyList[String], output: Schema[B]) extends Lambda[A, B]

  final case class IfElse[A, B](f: A ~> Boolean, ifTrue: A ~> B, ifFalse: A ~> B) extends Lambda[A, B]

  final case class Combine[A, B1, B2](left: A ~> B1, right: A ~> B2, o1: Schema[B1], o2: Schema[B2])
      extends Lambda[A, (B1, B2)]

  case class NumericOperation[A, B](
    operation: Numeric.Operation,
    left: A ~> B,
    right: A ~> B,
    num: Numeric.IsNumeric[B],
    schema: Schema[Numeric.IsNumeric[B]],
  ) extends Lambda[A, B]

  case class LogicalOperation[A](operation: Logical.Operation, left: A ~> Boolean, right: A ~> Boolean)
      extends Lambda[A, Boolean]
}