package spde

import sbt._

trait SpdeAndroidProject extends SpdeProject {
  override def spde_artifact = super.spde_artifact intransitive()
  val spde_core = "org.processing" % "core-android" % "1.1"
}