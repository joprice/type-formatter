package com.joprice

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc._
import scala.tools.nsc.typechecker.Analyzer


trait Formatter { self: Analyzer =>
  import global._
  import definitions._

  def isSymbolic(sym: Symbol) = sym.name.encodedName.toString != sym.name.decodedName.toString

  def printSym(sym: Symbol) = sym.name.decodedName.toString

  def formatInfix(t: Type): String = t match {
    case TypeRef(_, sym, List(left, right)) if isSymbolic(sym) =>
      s"${formatInfix(left)} ${printSym(sym)} ${formatInfix(right)}"
    case TypeRef(_, sym, args) if args.nonEmpty =>
      args.map(formatInfix).mkString(s"${printSym(sym)}[", ",", "]")
    case other =>
      other.toLongString
  }

}

class TypeFormatterPlugin(val global: Global) extends Plugin { plugin =>

  val analyzer = new {
    val global = plugin.global
  } with Analyzer with Formatter {
    import global._

    override def NoImplicitFoundError(tree: Tree, param: Symbol)(implicit context: Context): Unit = {
      def errMsg = {
        val paramName = param.name
        val paramTp = param.tpe
        def evOrParam = (
          if (paramName startsWith nme.EVIDENCE_PARAM_PREFIX)
            "evidence parameter of type"
          else
            s"parameter $paramName:")
        val symbol = paramTp.typeSymbolDirect
        symbol match {
          //TODO: override here for implicitNotFound annotations
          case ImplicitNotFoundMsg(msg) =>
            def typeArgsAtSym(paramTp: Type) = paramTp.baseType(symbol).typeArgs
            msg.format(typeArgsAtSym(paramTp).map(formatInfix))
          case _ => s"could not find implicit value for $evOrParam ${formatInfix(paramTp)}"
        }
      }
      ErrorUtils.issueNormalTypeError(tree, errMsg)
    }

    import global._

    private object DealiasedType extends TypeMap {
      def apply(tp: Type): Type = tp match {
        // Avoid "explaining" that String is really java.lang.String,
        // while still dealiasing types from non-default namespaces.
        case TypeRef(pre, sym, args) if sym.isAliasType && !sym.isInDefaultNamespace =>
          mapOver(tp.dealias)
        case _ =>
          mapOver(tp)
      }
    }

    // selected helpers taken from lihaoyi's pprint https://github.com/lihaoyi/upickle-pprint/blob/c3227d34547fe974a47f74f537be4cf6eaefbc22/pprint/shared/src/main/scala-2.11/pprint/TPrintImpl.scala
    override def foundReqMsg(found: Type, req: Type): String = {
      def explainAlias(tp: Type) = {
        // Don't automatically normalize standard aliases; they still will be
        // expanded if necessary to disambiguate simple identifiers.
        val deepDealias = DealiasedType(tp)
        if (tp eq deepDealias) "" else {
          // A sanity check against expansion being identical to original.
          val s = "" + formatInfix(deepDealias)
          if (s == "" + tp) ""
          else "\n    (which expands to)  " + s
        }
      }

      //TODO: restore long string logic (found.toLongString)
      val formatFound = formatInfix(found) + existentialContext(found) + explainAlias(found)
      val formatReq =  req + existentialContext(req) + explainAlias(req)

      def baseMessage =
        ";\n found   : " + formatFound +
         "\n required: " + formatReq
      (   withDisambiguation(Nil, found, req)(baseMessage)
        + explainVariance(found, req)
        + explainAnyVsAnyRef(found, req)
        )
    }
  }

  // taken from the original 2712 plugin https://github.com/milessabin/si2712fix-plugin/blob/5e25036f2353fed789520e55dd16284bd5982676/plugin/src/main/scala/si2712fix/Plugin.scala#L24

  val analyzerField = classOf[Global].getDeclaredField("analyzer")
  analyzerField.setAccessible(true)
  analyzerField.set(global, analyzer)

  val phasesSetMapGetter = classOf[Global].getDeclaredMethod("phasesSet")
  val phasesSet = phasesSetMapGetter.invoke(global).asInstanceOf[scala.collection.mutable.Set[SubComponent]]
  if (phasesSet.exists(_.phaseName == "typer")) { // `scalac -help` doesn't instantiate standard phases
  def subcomponentNamed(name: String) = phasesSet.find(_.phaseName == name).head
    val oldScs @ List(oldNamer, oldPackageobjects, oldTyper) = List(subcomponentNamed("namer"), subcomponentNamed("packageobjects"), subcomponentNamed("typer"))
    val newScs = List(analyzer.namerFactory, analyzer.packageObjects, analyzer.typerFactory)
    phasesSet --= oldScs
    phasesSet ++= newScs
  }

  val name = "type-formatter"
  val description = "Formats types in compilation errors"
  val components = Nil

}

