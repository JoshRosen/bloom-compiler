package edu.berkeley.cs.boom.bloomscala.codegen

import edu.berkeley.cs.boom.bloomscala.ast._
import edu.berkeley.cs.boom.bloomscala.analysis.{DepAnalyzer, Stratifier}

/**
 * Base class for code generators.
 */
abstract class CodeGenerator extends org.kiama.output.PrettyPrinter {

  /**
   * Compile a program.
   *
   * @param program a typechecked, name-resolved program.
   * @param stratifier a Stratifier, for checking rules' strata.
   * @param depAnalyzer a dependency analyzer, for checking rules' dependencies.
   *
   * @return A compiled program.
   */
  def generateCode(program: Program, stratifier: Stratifier, depAnalyzer: DepAnalyzer): CharSequence
}
