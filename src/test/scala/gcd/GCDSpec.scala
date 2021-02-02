// See README.md for license details.

package gcd

import chisel3._
import firrtl._
import chisel3.tester._
import org.scalatest.FreeSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.internal._
import chiseltest.experimental.TestOptionBuilder._
import firrtl.options.{StageMain}

import mappable._
import cliparams._

class GCDSpec(params: GCDConfig, annotations: AnnotationSeq = Seq()) extends FreeSpec with ChiselScalatestTester {

  "Gcd should calculate proper greatest common denominator" in {
    test(new GCD(params)) { dut =>
      dut.io.value1.poke(95.U)
      dut.io.value2.poke(10.U)
      dut.io.loadingValues.poke(true.B)
      dut.clock.step(1)
      dut.io.loadingValues.poke(false.B)
      while (dut.io.outputValid.peek().litToBoolean != dut.conf.validHigh) {
        dut.clock.step(1)
      }
      dut.io.outputGCD.expect(5.U)
    }
  }
}

class GCDTestStage extends GenericParameterCliStage[GCDConfig]((params, annotations) => {
  (new GCDSpec(params, annotations)).execute()}, GCDConfig())

object GCDTest extends StageMain(new GCDTestStage)
