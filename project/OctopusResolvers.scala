import sbt._
import Keys._

object OctopusResolvers {
  val aliyunPublic    = "Aliyun Public" at " https://maven.aliyun.com/repository/public"
  val aliyunCenter  = "Aliyun Center" at "http://download.java.net/maven/glassfish"
  val huaweicloudMaven = "Huaweicloud Maven" at "https://repo.huaweicloud.com/repository/maven/"

  val allResolver: Seq[MavenRepository] = Seq(Resolver.mavenLocal,
    aliyunPublic.withAllowInsecureProtocol(true),
    aliyunCenter.withAllowInsecureProtocol(true),
    huaweicloudMaven.withAllowInsecureProtocol(true))
}