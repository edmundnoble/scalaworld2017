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

object ASTObject {

  val ordinary = new ASTObject[Int] {
    def plus(a: Int, b: Int) = a + b

    def mult(a: Int, b: Int) = a * b

    def lit(n: Int) = n
  }

  val backwards = new ASTObject[Int] {
    def plus(a: Int, b: Int) = a * b

    def mult(a: Int, b: Int) = a + b

    def lit(n: Int) = n
  }

  val first = new ASTObject[Int] {
    def plus(a: Int, b: Int) = a

    def mult(a: Int, b: Int) = a

    def lit(n: Int) = n
  }
}

final case class ASTObjectRecord[@specialized(Int) A](plus: (A, A) => A, mult: (A, A) => A, lit: Int => A)

object ASTObjectRecord {
  def const[A, B](a: A, b: B): A = a
  val ordinary = new ASTObjectRecord[Int](_ + _, _ * _, identity[Int])
  val backwards = new ASTObjectRecord[Int](_ * _, _ + _, identity[Int])
  val first = new ASTObjectRecord[Int](const, const, identity[Int])
}

class Main {
  @Benchmark
  def plus1000TimesObject(bh: Blackhole): Unit = {
    bh.consume((0 to 5000).foldLeft(ASTObject.ordinary.lit(1) + ASTObject.backwards.lit(1)) { (a, b) =>
      if (a % 2 == 0) 
        ASTObject.ordinary.plus(a, b) + ASTObject.backwards.plus(a, b) + ASTObject.first.plus(a, b)
      else 
        ASTObject.ordinary.mult(a, b) + ASTObject.backwards.mult(a, b) + ASTObject.first.mult(a, b)
    })
  }

  @Benchmark
  def plus1000TimesObjectRecord(bh: Blackhole): Unit = {
    bh.consume((0 to 5000).foldLeft(ASTObjectRecord.ordinary.lit(1) + ASTObjectRecord.backwards.lit(1)) { (a, b) =>
      if (a % 2 == 0) 
        ASTObjectRecord.ordinary.plus(a, b) + ASTObjectRecord.backwards.plus(a, b) + ASTObjectRecord.first.plus(a, b)
      else 
        ASTObjectRecord.ordinary.mult(a, b) + ASTObjectRecord.backwards.mult(a, b) + ASTObjectRecord.first.mult(a, b)
    })
  }

  def interpretData(data: ASTData): Int = sumAdd(data) + productAdd(data) + firstAdd(data)

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

  def firstAdd(data: ASTData): Int = {
    data match {
      case Plus(a, b) => firstAdd(a)
      case Mult(a, b) => firstAdd(a)
      case Lit(n) => n
    }
  }

  @Benchmark
  def plus1000TimesAst(bh: Blackhole): Unit = {
    bh.consume(interpretData((0 to 5000).foldLeft(Lit(1): ASTData)((l, i) => if (i % 2 == 0) Plus(l, Lit(i)) else Mult(l, Lit(i)))))
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

