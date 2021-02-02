// See README.md for license details.

package gcd

import firrtl._
import chisel3._
import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation}
import firrtl.options.{StageMain}

import mappable._
import cliparams._

case class GCDConfig(
  len: Int = 16,
  validHigh: Boolean = true
)

/**
  * Compute GCD using subtraction method.
  * Subtracts the smaller from the larger until register y is zero.
  * value in register x is then the GCD
  */
class GCD (val conf: GCDConfig = GCDConfig()) extends Module {
  val io = IO(new Bundle {
    val value1        = Input(UInt(conf.len.W))
    val value2        = Input(UInt(conf.len.W))
    val loadingValues = Input(Bool())
    val outputGCD     = Output(UInt(conf.len.W))
    val outputValid   = Output(Bool())
  })

  val x  = Reg(UInt())
  val y  = Reg(UInt())

  when(x > y) { x := x - y }
    .otherwise { y := y - x }

  when(io.loadingValues) {
    x := io.value1
    y := io.value2
  }

  io.outputGCD := x
  if (conf.validHigh) {
    io.outputValid := y === 0.U
  } else {
    io.outputValid := y =/= 0.U
  }
}

class GCDGenStage extends GenericParameterCliStage[GCDConfig]((params, annotations) => {
  (new chisel3.stage.ChiselStage).execute(
    Array("-X", "verilog"),
    Seq(ChiselGeneratorAnnotation(() => new GCD(params))))}, GCDConfig())

object GCDGen extends StageMain(new GCDGenStage)
