package cliparams

import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation, ChiselCli}
import firrtl.AnnotationSeq
import firrtl.annotations.{Annotation, NoTargetAnnotation}
import firrtl.options.{HasShellOptions, Shell, ShellOption, Stage, Unserializable, StageMain}
import firrtl.stage.FirrtlCli

import mappable._

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

class GenericParameterCliStage[P: Mappable](thunk: (P, AnnotationSeq) => Unit, default: P) extends Stage {

  def mapify(p: P) = implicitly[Mappable[P]].toMap(p)
  def materialize(map: Map[String, String]) = implicitly[Mappable[P]].fromMap(map)

  val shell: Shell = new Shell("chiseltest") with ParameterCli with ChiselCli with FirrtlCli

  def run(annotations: AnnotationSeq): AnnotationSeq = {
    val params = annotations
      .collectFirst {case ParameterAnnotation(map) => materialize(mapify(default) ++ map.toSeq)}
      .getOrElse(default)

    thunk(params, annotations)
    annotations
  }
}
