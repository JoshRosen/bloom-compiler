package edu.berkeley.cs.boom.bloomscala

import com.typesafe.scalalogging.slf4j.Logging
import org.kiama.util.Messaging
import edu.berkeley.cs.boom.bloomscala.ast._
import edu.berkeley.cs.boom.bloomscala.parser.BudParser
import edu.berkeley.cs.boom.bloomscala.analysis._
import edu.berkeley.cs.boom.bloomscala.codegen.js.RxJsCodeGenerator
import com.quantifind.sumac.{ArgMain, FieldArgs}
import java.io.File
import com.quantifind.sumac.validation.Required
import scala.io.Source
import edu.berkeley.cs.boom.bloomscala.codegen.CodeGenerator
import edu.berkeley.cs.boom.bloomscala.codegen.dataflow.{GraphvizDataflowPrinter, DataflowCodeGenerator}
import edu.berkeley.cs.boom.bloomscala.typing.Typer


class CompilerArgs extends FieldArgs {
  @Required
  var infile: File = null
  var target: String = "RxJS"
}


object Compiler extends Logging with ArgMain[CompilerArgs] {

  val messaging = new Messaging
  private val namer = new Namer(messaging)
  private val typer = new Typer(messaging)

  def nameAndType(src: CharSequence): Program = {
    messaging.resetmessages()
    try {
      val parseResults = BudParser.parseProgram(src)
      val named = namer.resolveNames(parseResults)
      named.statements.map(typer.isWellTyped)
      named
    } catch { case e: Exception =>
      logger.error("Compilation failed", e)
      throw e
    } finally {
      messaging.report()
      if (messaging.messagecount != 0) {
        // TODO: this is fine for now for simple tests, but in the future
        // `compile` should return more detailed information for consumption
        // by unit tests
        throw new CompilerException("Compilation had error messages")
      }
    }
  }

  /**
   * Compiles a program, but stops short of code generation.
   */
  def compile(src: CharSequence): Program = {
    val typed = nameAndType(src)
    val depAnalyzer = new DepAnalyzer(typed)
    val stratifier = new Stratifier(messaging, depAnalyzer)
    if (!stratifier.isTemporallyStratifiable(typed)) {
      throw new StratificationError("Program is unstratifiable")
    }
    typed
  }

  def generateCode(program: Program, generator: CodeGenerator): CharSequence = {
    val depAnalyzer = new DepAnalyzer(program)
    val stratifier = new Stratifier(messaging, depAnalyzer)
    generator.generateCode(program, stratifier, depAnalyzer)
  }

  def main(args: CompilerArgs) {
    val generator = args.target.toLowerCase match {
      case "rxjs" => RxJsCodeGenerator
      case "dataflow" => GraphvizDataflowPrinter
      case unknown => throw new IllegalArgumentException(s"Unknown target platform $unknown")
    }
    val program = compile(Source.fromFile(args.infile).mkString)
    val code = generateCode(program, generator)
    println(code)
  }
}