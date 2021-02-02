// See README.md for license details.

package gcd

import firrtl._
import chisel3._
import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation}
import chisel3.stage.ChiselCli
import firrtl.annotations.{Annotation, NoTargetAnnotation}
import firrtl.options.{HasShellOptions, Shell, ShellOption, Stage, Unserializable, StageMain}
import firrtl.stage.FirrtlCli

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

trait SomeAnnotaion {
  this: Annotation =>
}

case class ParameterAnnotation(map: Map[String, String])
    extends SomeAnnotaion
    with NoTargetAnnotation
    with Unserializable

object ParameterAnnotation extends HasShellOptions {
  val options = Seq(
    new ShellOption[Map[String, String]](
      longOption = "params",
      toAnnotationSeq = (a: Map[String, String]) => Seq(ParameterAnnotation(a)),
      helpText = """a comma separated, space free list of additional paramters, e.g. --param-string "k1=7,k2=dog" """
    )
  )
}

trait ParameterCli {
  this: Shell =>

  Seq(ParameterAnnotation).foreach(_.addOptions(parser))
}

import mappable._
class GenericParameterCliStage[P: Mappable](thunk: (P, AnnotationSeq) => Unit, default: P) extends Stage {

  def materialize(map: Map[String, String]) = implicitly[Mappable[P]].fromMap(map)

  val shell: Shell = new Shell("chiseltest") with ParameterCli with ChiselCli with FirrtlCli

  def run(annotations: AnnotationSeq): AnnotationSeq = {
    val params = annotations
      .collectFirst {case ParameterAnnotation(map) => materialize(map)}
      .getOrElse(default)

    thunk(params, annotations)
    annotations
  }
}

class GCDGenStage extends GenericParameterCliStage[GCDConfig]((params, annotations) => {
  (new chisel3.stage.ChiselStage).execute(
    Array("-X", "verilog"),
    Seq(ChiselGeneratorAnnotation(() => new GCD(params))))}, GCDConfig())

object GCDGen extends StageMain(new GCDGenStage)
