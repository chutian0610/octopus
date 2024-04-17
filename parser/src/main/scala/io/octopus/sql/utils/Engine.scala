package io.octopus.sql.utils
case class EngineInstance(engineType:Engine, version:Version)
enum Engine{
  case PRESTO
}

sealed trait Version{
  def matches(version:Int):Boolean
}

case class AllVersion() extends Version{
  override def matches(version: Int): Boolean = true

}
case class SimpleVersion(value:Int) extends Version{
  override def matches(version: Int): Boolean = {
    value == version
  }
}
case class RangeVersion(min:Option[Int],max:Option[Int]) extends Version{
  override def matches(version: Int): Boolean = {
    val minCheck = if(min.isDefined) min.get <= version else true
    val maxCheck = if(max.isDefined) max.get >= version else true
    minCheck && maxCheck
  }
}
