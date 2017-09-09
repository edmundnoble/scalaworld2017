package sbench

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.infra.Blackhole

trait ASTData

case class Plus(a: ASTData, b: ASTData) extends ASTData

case class Mult(a: ASTData, b: ASTData) extends ASTData

case class Lit(n: Int) extends ASTData

trait ASTObject[A] {
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

case class ASTObjectRecord[A](plus: (A, A) => A, mult: (A, A) => A, lit: Int => A)

object ASTObjectRecord {
  val int = ASTObjectRecord[Int](_ + _, _ * _, identity[Int])
  val otherInt = ASTObjectRecord[Int](_ * _, _ + _, identity[Int])
}

class Main {
  @Benchmark
  def plus1000TimesObject(bh: Blackhole): Unit = {
    bh.consume((0 to 14000).foldLeft(astObject.lit(1) + astOtherObject.lit(1)) { (a, b) =>
      val o = if (a % 2 == 0) astObject else astOtherObject
      if (a % 3 == 0) o.plus(a, b) else o.mult(a, b)
    })
  }

  @Benchmark
  def plus1000TimesObjectRecord(bh: Blackhole): Unit = {
    bh.consume((0 to 14000).foldLeft(ASTObjectRecord.int.lit(1) + ASTObjectRecord.otherInt.lit(1)) { (a, b) =>
      val o = if (a % 2 == 0) ASTObjectRecord.int else ASTObjectRecord.otherInt
      o.mult(o.lit(a), o)
      if (a % 3 == 0) o.plus(a, b) else o.mult(a, b)
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

}

