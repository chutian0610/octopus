package io.octopus.sql.utils
enum Engine{
  case PRESTO_DB, MYSQL, STARROCKS
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

case class EngineInstance(engineType:Engine, version:Version)