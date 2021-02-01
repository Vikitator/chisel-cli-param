// See README.md for license details.

package gcd

import firrtl._
import chisel3._

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

trait HasParams {
  self: ExecutionOptionsManager =>

  var params: Map[String, String] = Map()

  parser.note("Design Parameters")

  parser.opt[Map[String, String]]('p', "params")
    .valueName("k1=v1,k2=v2")
    .foreach { v => params = v }
    .text("Parameters of Predictor")
}

object GCD {
  def apply(params: Map[String, String]): GCD = {
    new GCD(params2conf(params))
  }

  def params2conf(params: Map[String, String]): GCDConfig = {
    var conf = new GCDConfig
    for ((k, v) <- params) {
      (k, v) match {
        case ("len", _) => conf = conf.copy(len = v.toInt)
        case ("validHigh", _) => conf = conf.copy(validHigh = v.toBoolean)
        case _ =>
      }
    }
    conf
  }
}

object GCDGen extends App {
  import mappable._

  def mapify[T: Mappable](t: T) = implicitly[Mappable[T]].toMap(t)
  def materialize[T: Mappable](map: Map[String, String]) = implicitly[Mappable[T]].fromMap(map)

  val optionsManager = new ExecutionOptionsManager("gcdgen")
  with HasChiselExecutionOptions with HasFirrtlOptions with HasParams
  optionsManager.parse(args) match {
    case true => 
      chisel3.Driver.execute(optionsManager, () => new GCD(materialize[GCDConfig](optionsManager.params)))
    case _ =>
      ChiselExecutionFailure("could not parse results")
  }
}
