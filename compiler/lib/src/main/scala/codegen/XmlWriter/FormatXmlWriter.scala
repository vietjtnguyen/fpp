package fpp.compiler.codegen

import fpp.compiler.analysis._
import fpp.compiler.ast._
import fpp.compiler.util._

/** Write an FPP format as XML */
object FormatXmlWriter {

  sealed trait Signedness
  case object Signed extends Signedness
  case object Unsigned extends Signedness

  def signedness(typeInt: Ast.TypeInt): Signedness = typeInt match {
    case Ast.I8() => Signed
    case Ast.I16() => Signed
    case Ast.I32() => Signed
    case Ast.I64() => Signed
    case Ast.U8() => Unsigned
    case Ast.U16() => Unsigned
    case Ast.U32() => Unsigned
    case Ast.U64() => Unsigned
  }

  /** Convert a format field and type name to a string */
  def fieldToString(f: Format.Field, tn: AstNode[Ast.TypeName]): String = {
    import Format.Field._
    def default = tn.data match {
      case Ast.TypeNameFloat(name) => "%g"
      case Ast.TypeNameInt(typeInt) => signedness(typeInt) match {
        case Signed => "%d"
        case Unsigned => "%u"
      }
      case Ast.TypeNameQualIdent(name) => "%s"
      case Ast.TypeNameBool => "%u"
      case Ast.TypeNameString(size) => "%s"
    }
    def integer(t: Integer.Type) = {
      val Ast.TypeNameInt(typeInt) = tn.getData
      (t, signedness(typeInt)) match {
        case (Integer.Binary, _) => "%x"
        case (Integer.Character, _) => "%c"
        case (Integer.Decimal, Signed) => "%d"
        case (Integer.Decimal, Unsigned) => "%u"
        case (Integer.Hexadecimal, _) => "%x"
        case (Integer.Octal, _) => "%o"
      }
    }
    def floating(precision: Option[Int], t: Floating.Type) = {
      val precisionStr = precision match {
        case Some(p) => s".${p.toString}"
        case None => ""
      }
      t match {
        case Floating.Exponent => s"%${precisionStr}e"
        case Floating.Fixed => s"%${precisionStr}f"
        case Floating.General => s"%${precisionStr}g"
        case Floating.Percent => s"%${precisionStr}f"
      }
    }
    f match {
      case Default => default
      case Integer(t) => integer(t)
      case Floating(precision, t) => floating(precision, t)
    }
  }

  /** Convert a format to a string */
  def formatToString(f: Format, nodes: List[AstNode[Ast.TypeName]]): String = {
    val fields = f.fields
    if (fields.length != nodes.length) 
      throw new InternalError("number of nodes should match number of fields")
    val pairs = fields zip nodes
    pairs.foldLeft(f.prefix)({ case (s, ((field, s1), tn)) => s ++ fieldToString(field, tn) ++ s1 })
  }

}
