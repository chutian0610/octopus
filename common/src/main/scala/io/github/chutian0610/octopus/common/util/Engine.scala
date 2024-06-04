package io.github.chutian0610.octopus.common.util

enum Engine{
  case OCTOPUS,PRESTO_DB, MYSQL, STAR_ROCKS
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

case class Service(engineType:Engine, version:Version)