package compose.dsl

import compose._
import compose.Lambda.constant

trait ConsoleDSL {
  def writeLine: String ~> Unit = Lambda.make[String, Unit] { ExecutionPlan.Console.WriteLine }

  def writeLine(text: String): Any ~> Unit = constant(text) >>> writeLine

  def readLine: Any ~> String = Lambda.make[Any, String] { ExecutionPlan.Console.ReadLine(None) }

  def readLine(prompt: String): Any ~> String = Lambda.make[Any, String] {
    ExecutionPlan.Console.ReadLine(Some(prompt))
  }
}