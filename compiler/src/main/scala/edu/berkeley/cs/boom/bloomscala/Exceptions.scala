package edu.berkeley.cs.boom.bloomscala

class CompilerException(msg: String) extends Exception(msg)
class StratificationError(msg: String) extends CompilerException(msg)