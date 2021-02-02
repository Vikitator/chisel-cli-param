package mappable

import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

trait Mappable[T] {
  def toMap(t: T): Map[String, String]
  def fromMap(map: Map[String, String]): T
}

object Mappable {
  implicit def materializeMappable[T]: Mappable[T] = macro materializeMappableImpl[T]

  def materializeMappableImpl[T: c.WeakTypeTag](c: Context): c.Expr[Mappable[T]] = {
    import c.universe._
    val tpe = weakTypeOf[T]
    val companion = tpe.typeSymbol.companion

    val fields = tpe.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    }.get.paramLists.head

    val (toMapParams, fromMapParams) = fields.map { field =>
      val name = field.name.toTermName
      val decoded = name.decodedName.toString
      val returnType = tpe.decl(name).typeSignature

      val fromMapLine = returnType match {
        // https://groups.google.com/g/scala-user/c/XElKxcK39I://groups.google.com/g/scala-user/c/XElKxcK39Ik 
        case NullaryMethodType(res) if res =:= typeOf[Int] => q"map($decoded).toInt"
        case NullaryMethodType(res) if res =:= typeOf[String] => q"map($decoded)"
        case NullaryMethodType(res) if res =:= typeOf[Boolean] => q"map($decoded).toBoolean"
        case _ => q""
      }

      (q"$decoded -> t.$name.toString", fromMapLine)
    }.unzip

    c.Expr[Mappable[T]] { q"""
      new Mappable[$tpe] {
        def toMap(t: $tpe): Map[String, String] = Map(..$toMapParams)
        def fromMap(map: Map[String, String]): $tpe = $companion(..$fromMapParams)
      }
    """ }
  }
}
