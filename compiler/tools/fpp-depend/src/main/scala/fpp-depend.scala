package fpp.compiler.tools

import fpp.compiler.analysis._
import fpp.compiler.ast._
import fpp.compiler.codegen._
import fpp.compiler.syntax._
import fpp.compiler.transform._
import fpp.compiler.util._
import scopt.OParser

object FPPDepend {

  case class Options(
    directFile: Option[String] = None,
    files: List[File] = List(),
    generatedFile: Option[String] = None,
    includedFile: Option[String] = None,
    missingFile: Option[String] = None
  )

  def mapIterable[T](it: Iterable[T], f: String => Unit) =
    it.map(_.toString).toArray.sortWith(_ < _).map(f)

  def command(options: Options) = {
    val files = options.files.reverse match {
      case Nil => List(File.StdIn)
      case list => list
    }
    val a = Analysis(inputFileSet = options.files.toSet)
    for {
      tul <- Result.map(files, Parser.parseFile (Parser.transUnit) (None) _)
      aTul <- ResolveSpecInclude.transformList(
        a,
        tul,
        ResolveSpecInclude.transUnit
      )
      a <- Right(aTul._1)
      tul <- Right(aTul._2)
      a <- ComputeDependencies.tuList(a, tul)
      _ <- options.directFile match {
        case Some(file) => writeFiles(a.directDependencyFileSet, file)
        case None => Right(())
      }
      _ <- options.generatedFile match {
        case Some(file) =>
          for (files <- ComputeGeneratedFiles.getFiles(tul))
            yield writeFiles(files, file)
        case None => Right(())
      }
      _ <- options.includedFile match {
        case Some(file) => writeFiles(a.includedFileSet, file)
        case None => Right(())
      }
      _ <- options.missingFile match {
        case Some(file) => writeFiles(a.missingDependencyFileSet, file)
        case None => Right(())
      }
    } yield mapIterable(a.dependencyFileSet, System.out.println(_))
  }

  def writeFiles[T](files: Iterable[T], fileName: String): Result.Result[Unit] = {
    val file = File.fromString(fileName)
    for { writer <- file.openWrite() 
    } yield { 
      mapIterable(files, writer.println(_))
      writer.close()
      ()
    }
  }

  def main(args: Array[String]) = {
    Error.setTool(Tool(name))
    val options = OParser.parse(oparser, args, Options())
    for { result <- options } yield {
      command(result) match {
        case Left(error) => {
          error.print
          System.exit(1)
        }
        case _ => ()
      }
    }
    ()
  }

  val builder = OParser.builder[Options]

  val name = "fpp-depend"

  val oparser = {
    import builder._
    OParser.sequence(
      programName(name),
      head(name, Version.v),
      opt[String]('d', "direct")
        .valueName("<file>")
        .action((m, c) => c.copy(directFile = Some(m)))
        .text("write direct dependencies to file"),
      opt[String]('g', "generated")
        .valueName("<file>")
        .action((m, c) => c.copy(generatedFile = Some(m)))
        .text("write names of generated files to file"),
      opt[String]('i', "included")
        .valueName("<file>")
        .action((m, c) => c.copy(includedFile = Some(m)))
        .text("write included dependencies to file"),
      opt[String]('m', "missing")
        .valueName("<file>")
        .action((m, c) => c.copy(missingFile = Some(m)))
        .text("write missing dependencies to file"),
      help('h', "help").text("print this message and exit"),
      arg[String]("file ...")
        .unbounded()
        .optional()
        .action((f, c) => c.copy(files = File.fromString(f) :: c.files))
        .text("input files"),
    )
  }

}
