package com.tusharmath.compose

import com.tusharmath.compose.Lambda.Pipe
import zio.prelude.NonEmptyList
import zio.schema.{AccessorBuilder, Schema}

sealed trait Lambda[A, B] { self =>
  def <<<[X](other: Lambda[X, A]): Lambda[X, B] = self compose other

  def compose[X](other: Lambda[X, A]): Lambda[X, B] = Pipe(other, self)

  def >>:(a: A)(implicit schema: Schema[A]): Unit ~> B = call(a)

  def call(a: A)(implicit schema: Schema[A]): Unit ~> B = Lambda(a) >>> self

  def >>>[C](other: Lambda[B, C]): Lambda[A, C] = self pipe other

  def pipe[C](other: Lambda[B, C]): Lambda[A, C] = Pipe(self, other)

  def &&[A1, B1](
    other: Lambda[A1, B1],
  )(implicit
    a1: Schema[A],
    a2: Schema[A1],
    b1: Schema[B],
    b2: Schema[B1],
  ): (A, A1) ~> (B, B1) = self zip other

  def zip[A1, B1](
    other: Lambda[A1, B1],
  )(implicit
    a1: Schema[A],
    a2: Schema[A1],
    b1: Schema[B],
    b2: Schema[B1],
  ): (A, A1) ~> (B, B1) =
    Lambda.zip(self, other)

  private[compose] def executable: ExecutionPlan =
    ExecutionPlan.fromLambda(self)
}

object Lambda {

  def ifElse[A, B](f: A ~> Boolean)(isTrue: A ~> B, isFalse: A ~> B): A ~> B =
    IfElse(f, isTrue, isFalse)

  def fromMap[A, B](
    source: Map[A, B],
  )(implicit input: Schema[A], output: Schema[B]): Lambda[A, B] =
    FromMap(input, source, output)

  def inc: Int ~> Int = Lambda.partial2(add, 1)

  def call[A, B](f: A ~> B, a: A)(implicit ev: Schema[A]): Unit ~> B = f.call(a)

  def dec: Int ~> Int = Lambda.partial2(add, -1)

  def add: (Int, Int) ~> Int = AddInt

  def partial2[A1, A2, B](f: (A1, A2) ~> B, a1: A1)(implicit
    s1: Schema[A1],
    s2: Schema[A2],
  ): A2 ~> B =
    Partial21(f, a1, s1, s2)

  def mul: (Int, Int) ~> Int = MulInt

  def partial1[A1, B](f: A1 ~> B, a1: A1)(implicit s1: Schema[A1]): Unit ~> B =
    Partial11(f, a1, s1)

  def partial2[A1, A2, B](f: (A1, A2) ~> B, a1: A1, a2: A2)(implicit
    s1: Schema[A1],
    s2: Schema[A2],
  ): Unit ~> B =
    Partial22(f, a1, a2, s1, s2)

  def identity[A]: Lambda[A, A] = Identity[A]()

  def useWith[A1, B1, A2, B2, B](f: (B1, B2) ~> B)(f1: A1 ~> B1, f2: A2 ~> B2)(
    implicit
    a1: Schema[A1],
    a2: Schema[A2],
    b1: Schema[B1],
    b2: Schema[B2],
  ): (A1, A2) ~> B =
    (f1 zip f2) >>> f

  def zip[A1, A2, B1, B2](f1: A1 ~> B1, f2: A2 ~> B2)(implicit
    a1: Schema[A1],
    a2: Schema[A2],
    b1: Schema[B1],
    b2: Schema[B2],
  ): (A1, A2) ~> (B1, B2) =
    Zip2(f1, f2, a1, a2, b1, b2)

  def unit: Lambda[Unit, Unit] = Lambda(())

  def apply[B](a: B)(implicit schema: Schema[B]): Lambda[Unit, B] = always(a)

  def always[B](a: B)(implicit schema: Schema[B]): Lambda[Unit, B] =
    Always(a, schema)

  final case class FromMap[A, B](
    input: Schema[A],
    source: Map[A, B],
    output: Schema[B],
  ) extends Lambda[A, B]
  final case class Always[B](b: B, schema: Schema[B]) extends Lambda[Unit, B]
  final case class Identity[A]() extends Lambda[A, A]
  final case class Pipe[A, B, C](f: Lambda[A, B], g: Lambda[B, C])
      extends Lambda[A, C]
  final case class Zip2[A1, A2, B1, B2](
    f1: A1 ~> B1,
    f2: A2 ~> B2,
    i1: Schema[A1],
    i2: Schema[A2],
    o1: Schema[B1],
    o2: Schema[B2],
  ) extends Lambda[(A1, A2), (B1, B2)]
  final case class Select[A, B](
    input: Schema[A],
    path: NonEmptyList[String],
    output: Schema[B],
  ) extends Lambda[A, B]
  final case class Partial11[A1, B](f: A1 ~> B, a1: A1, s1: Schema[A1])
      extends Lambda[Unit, B]
  final case class Partial21[A1, A2, B](
    f: (A1, A2) ~> B,
    a1: A1,
    s1: Schema[A1],
    s2: Schema[A2],
  ) extends Lambda[A2, B]
  final case class Partial22[A1, A2, B](
    f: (A1, A2) ~> B,
    a1: A1,
    a2: A2,
    s1: Schema[A1],
    s2: Schema[A2],
  ) extends Lambda[Unit, B]

  final case class IfElse[A, B](
    f: A ~> Boolean,
    isTrue: A ~> B,
    isFalse: A ~> B,
  ) extends Lambda[A, B]

  final case object AddInt extends Lambda[(Int, Int), Int]

  final case object MulInt extends Lambda[(Int, Int), Int]

  object Accessors extends AccessorBuilder {
    override type Lens[S, A]      = Lambda[S, A]
    override type Prism[S, A]     = Unit
    override type Traversal[S, A] = Unit

    override def makeLens[S, A](
      product: Schema.Record[S],
      term: Schema.Field[A],
    ): Lens[S, A] =
      Lambda.Select(product, NonEmptyList(term.label), term.schema)

    override def makePrism[S, A](
      sum: Schema.Enum[S],
      term: Schema.Case[A, S],
    ): Prism[S, A] = ()

    override def makeTraversal[S, A](
      collection: Schema.Collection[S, A],
      element: Schema[A],
    ): Traversal[S, A] = ()
  }

}