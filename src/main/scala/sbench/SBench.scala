package sbench

import cats.Eval
import cats.syntax.apply._
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.infra.Blackhole

trait ASTData

case class Plus(a: ASTData, b: ASTData) extends ASTData

case class Mult(a: ASTData, b: ASTData) extends ASTData

case class Lit(n: Int) extends ASTData

trait ASTObject[@specialized(Int) A] {
  def plus(a: A, b: A): A

  def mult(a: A, b: A): A

  def lit(n: Int): A
}

object astObject extends ASTObject[Int] {
  def plus(a: Int, b: Int) = a + b

  def mult(a: Int, b: Int) = a * b

  def lit(n: Int) = n
}

object astOtherObject extends ASTObject[Int] {
  def plus(a: Int, b: Int) = a * b

  def mult(a: Int, b: Int) = a + b

  def lit(n: Int) = n
}

final case class ASTObjectRecord[@specialized(Int) A](plus: (A, A) => A, mult: (A, A) => A, lit: Int => A)

object ASTObjectRecord {
  val int = ASTObjectRecord[Int](_ + _, _ * _, identity[Int])
  val otherInt = ASTObjectRecord[Int](_ * _, _ + _, identity[Int])
}

class Main {
  @Benchmark
  def plus1000TimesObject(bh: Blackhole): Unit = {
    bh.consume((0 to 3000).foldLeft(astObject.lit(1) + astOtherObject.lit(1)) { (a, b) =>
      val o = if (a % 2 == 0) astObject else astOtherObject
      o.mult(o.plus(b, o.lit(a)), o.lit(a))
    })
  }

  @Benchmark
  def plus1000TimesObjectRecord(bh: Blackhole): Unit = {
    bh.consume((0 to 3000).foldLeft(ASTObjectRecord.int.lit(1) + ASTObjectRecord.otherInt.lit(1)) { (a, b) =>
      val o = if (a % 2 == 0) ASTObjectRecord.int else ASTObjectRecord.otherInt
      o.mult(o.plus(b, o.lit(a)), o.lit(a))
    })
  }

  def interpretData(data: ASTData): Int = sumAdd(data) + productAdd(data)

  def sumAdd(data: ASTData): Int = {
    data match {
      case Plus(a, b) => sumAdd(a) + sumAdd(b)
      case Mult(a, b) => sumAdd(a) * sumAdd(b)
      case Lit(n) => n
    }
  }

  def productAdd(data: ASTData): Int = {
    data match {
      case Plus(a, b) => productAdd(a) * productAdd(b)
      case Mult(a, b) => productAdd(a) + productAdd(b)
      case Lit(n) => n
    }
  }

  @Benchmark
  def plus1000TimesAst(bh: Blackhole): Unit = {
    bh.consume(interpretData((0 to 3000).foldLeft(Lit(1): ASTData)((l, i) => Mult(Plus(l, Lit(i)), Lit(i)))))
  }

  def interpretTrampolineData(data: ASTData): Int =
    sumTrampolineAdd(data).value + productTrampolineAdd(data).value

  def sumTrampolineAdd(data: ASTData): Eval[Int] = {
    data match {
      case Plus(a, b) => (Eval.defer(sumTrampolineAdd(a)), Eval.defer(sumTrampolineAdd(b))).mapN(_ + _)
      case Mult(a, b) => (Eval.defer(sumTrampolineAdd(a)), Eval.defer(sumTrampolineAdd(b))).mapN(_ * _)
      case Lit(n) => Eval.now(n)
    }
  }

  def productTrampolineAdd(data: ASTData): Eval[Int] = {
    data match {
      case Plus(a, b) => (Eval.defer(productTrampolineAdd(a)), Eval.defer(productTrampolineAdd(b))).mapN(_ * _)
      case Mult(a, b) => (Eval.defer(productTrampolineAdd(a)), Eval.defer(productTrampolineAdd(b))).mapN(_ + _)
      case Lit(n) => Eval.now(n)
    }
  }

  @Benchmark
  def plus1000TimesTrampolineAst(bh: Blackhole): Unit = {
    bh.consume(interpretTrampolineData((0 to 3000).foldLeft(Lit(1): ASTData)((l, i) => Mult(Plus(l, Lit(i)), Lit(i)))))
  }

}

