package compose.dsl

import compose.~>
import compose.Lambda.{constant, make}
import compose.ExecutionPlan.LogicalExecution
import compose.operation.LogicalOp

trait BooleanDSL[-A, +B] { self: A ~> B =>
  final def &&[A1](other: A1 ~> Boolean)(implicit ev: B <:< Boolean): A1 ~> Boolean =
    self and other

  final def ||[A1](other: A1 ~> Boolean)(implicit ev: B <:< Boolean): A1 ~> Boolean =
    self or other

  final def and[A1](other: A1 ~> Boolean)(implicit ev: B <:< Boolean): A1 ~> Boolean =
    make[A1, Boolean] {
      LogicalExecution(LogicalOp.And(self.compile, other.compile))
    }

  final def diverge[C](isTrue: B ~> C, isFalse: B ~> C)(implicit ev: B <:< Boolean): A ~> C =
    make[A, C] {
      LogicalExecution(LogicalOp.Diverge(self.compile, isTrue.compile, isFalse.compile))
    }

  final def eq[A1 <: A, B1 >: B](other: A1 ~> B1): A1 ~> Boolean =
    make[A1, Boolean] {
      LogicalExecution(LogicalOp.Equals(self.compile, other.compile))
    }

  final def isFalse(implicit ev: B <:< Boolean): A ~> Boolean =
    self =:= constant(false)

  final def isTrue(implicit ev: B <:< Boolean): A ~> Boolean =
    self =:= constant(true)

  final def not(implicit ev: B <:< Boolean): A ~> Boolean = make[A, Boolean] {
    LogicalExecution(LogicalOp.Not(self.compile))
  }

  final def or[A1](other: A1 ~> Boolean)(implicit ev: B <:< Boolean): A1 ~> Boolean =
    make[A1, Boolean] {
      LogicalExecution(LogicalOp.Or(self.compile, other.compile))
    }
}
