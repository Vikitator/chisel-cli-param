// See README.md for license details.

package gcd

import chisel3._
import firrtl._
import chisel3.tester._
import org.scalatest.FreeSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.internal._
import chiseltest.experimental.TestOptionBuilder._

object GCDTest extends App {
  val optionsManager = new ExecutionOptionsManager("gcdtest") with HasParams
  optionsManager.parse(args) match {
    case true => 
      //println(optionsManager.commonOptions.programArgs)
      (new GCDSpec(optionsManager.params)).execute()
    case _ =>
      ChiselExecutionFailure("could not parse results")
  }
}

/**
  * This is a trivial example of how to run this Specification
  * From within sbt use:
  * {{{
  * testOnly gcd.GcdDecoupledTester
  * }}}
  * From a terminal shell use:
  * {{{
  * sbt 'testOnly gcd.GcdDecoupledTester'
  * }}}
  */
class GCDSpec(params: Map[String, String] = Map()) extends FreeSpec with ChiselScalatestTester {

  "Gcd should calculate proper greatest common denominator" in {
    test(GCD(params)) { dut =>
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
