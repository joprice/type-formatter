
val scalaV = "2.11.8"

lazy val root = plugin.in(file("."))
  .settings(
    crossScalaVersions := Seq(scalaV, "2.10.6")
  )
  .aggregate(plugin, test)

lazy val commonSettings = Seq(
  scalaVersion := scalaV,
  organization := "com.joprice",
  version := "0.0.1-SNAPSHOT"
)

lazy val plugin = project
  .settings(
    name := "type-formatter"
  )
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    )
  )

// taken from https://github.com/milessabin/si2712fix-plugin/blob/master/build.sbt#L31
lazy val usePluginSettings = Seq(
  scalacOptions in Compile <++= (Keys.`package` in (plugin, Compile)) map { (jar: File) =>
    System.setProperty("sbt.paths.plugin.jar", jar.getAbsolutePath)
    val addPlugin = "-Xplugin:" + jar.getAbsolutePath
    val dummy = "-Jdummy=" + jar.lastModified
    Seq(addPlugin, dummy)
  }
)

lazy val test = project
  .settings(commonSettings)
  .settings(usePluginSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.1"
    )
  )
