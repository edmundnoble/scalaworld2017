package sbench

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.infra.Blackhole
class Main {
  @Benchmark
  def plus1000Times(bh: Blackhole): Int = {
    1
  }

}

