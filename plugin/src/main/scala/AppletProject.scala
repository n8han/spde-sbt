package spde

import sbt._

trait AppletProject extends SpdeProject with BasicPackagePaths with archetect.TemplateTasks
{
  // this project's Proguard invocation tasks are derived from sbt.LoaderProject
  import java.io.File
  val appletOutput = outputPath / "applet"
  val proguardConfigurationPath: Path = outputPath / "proguard.pro"
  def outputExt(ext: String) = name + "-applet-" + version + "." + ext

  lazy val outputJar = appletOutput / outputExt("jar")
  lazy val outputJp = outputPath / outputExt("jp")
  lazy val outputJpgz = appletOutput / outputExt("jpgz")
  lazy val outputHtml = appletOutput / (name + ".html")
  
  def rootProjectDirectory = rootProject.info.projectPath

  /****** Dependencies  *******/
  val defaultConfig = config("default")
  val toolsConfig = config("tools")
  val proguardJar = "net.sf.proguard" % "proguard" % "4.3" % "tools->default"
  
  /******** Proguard *******/
  lazy val proguard = proguardTask dependsOn(`package`, glob, writeProguardConfiguration)
  lazy val writeProguardConfiguration = writeProguardConfigurationTask dependsOn `package`
  lazy val pack = packTask dependsOn(proguard)
  lazy val writeHtml = writeHtmlTask dependsOn(glob)
  lazy val applet = appletTask dependsOn (pack, writeHtml)
  
  private def proguardTask = fileTask(outputJar from sourceGlob)
    {
      FileUtilities.clean(outputJar :: Nil, log)
      val proguardClasspath = managedClasspath(toolsConfig)
      val proguardClasspathString = Path.makeString(proguardClasspath.get)
      val configFile = proguardConfigurationPath.asFile.getAbsolutePath
      val exitValue = Process("java", List("-Xmx128M", "-cp", proguardClasspathString, "proguard.ProGuard", "@\"%s\"" format configFile)) ! log
      if(exitValue == 0) None else Some("Proguard failed with nonzero exit code (" + exitValue + ")")
    }
  private def renderInfo = {
    // tasks that call here should depend on glob
    val sz = """(?s).*size\s*\(\s*(\d+)\s*,\s*(\d+),?\s*(\S*)\s*\).*""".r
    val sz_line = io.Source.fromFile(sourceGlob.asFile).getLines.find(None != sz.findFirstIn(_))
    val rendererMap = Map (
      "P3D" -> "processing.core.PGraphics3D",
      "JAVA2D" -> "processing.core.PGraphicsJava2D",
      "OPENGL"-> "processing.opengl.PGraphicsOpenGL",
      "PDF"-> "processing.pdf.PGraphicsPDF",
      "DXF"-> "processing.dxf.RawDXF"
    )
    val univRenderers = "processing.core.PGraphicsJava2D" :: Nil
    
    sz_line match {
      case Some(sz(width, height, "")) => (width, height, univRenderers)
      case Some(sz(width, height, r)) => (width, height, rendererMap(r) :: univRenderers)
      case Some(sz(width, height)) => (width, height, univRenderers)
      case _ => (100, 100, univRenderers)
    }
  }
  private def writeProguardConfigurationTask = 
    fileTask(proguardConfigurationPath from sourceGlob)
    {
      // the template for the proguard configuration file
      val outTemplate = """
        |-dontobfuscate
        |-dontnote
        |-dontwarn
        |-libraryjars %s
        |%s
        |-outjars %s
        |-ignorewarnings
        |-keep class %s
        |-keep class MyRunner { *** main(...); }
        |-keep class processing.core.PApplet { *** main(...); }
        |-keep class spde.core.SApplet { *** scripty(...); }
        |-keepclasseswithmembers class * { public void dispose(); }
        |"""
      
      val defaultJar = (outputPath / defaultJarName).asFile.getAbsolutePath
      log.debug("proguard configuration using main jar " + defaultJar)
      val externalDependencies = Set() ++ (
        mainCompileConditional.analysis.allExternals ++ compileClasspath.get.map { _.asFile }
      ) map { _.getAbsoluteFile } filter {  _.getName.endsWith(".jar") }
      
      log.debug("proguard configuration external dependencies: \n\t" + externalDependencies.mkString("\n\t"))
      // partition jars from the external jar dependencies of this project by whether they are located in the project directory
      // if they are, they are specified with -injars, otherwise they are specified with -libraryjars
      val (externalJars, libraryJars) = externalDependencies.toList.partition(jar => Path.relativize(rootProjectDirectory, jar).isDefined)
      log.debug("proguard configuration library jars locations: " + libraryJars.mkString(", "))
      // exclude properties files and manifests from scala-library jar
      val inJars = (defaultJar :: externalJars.map( _ + "(!META-INF/**,!*.txt)")).map("-injars " + _).mkString("\n")
      
      val (width, height, renderers) = renderInfo
      
      val proguardConfiguration =
        outTemplate.stripMargin.format(libraryJars.mkString(File.pathSeparator),
          inJars, outputJar.absolutePath, name) + renderers.map { renderer =>
            "-keep class %s { *** <init>(...); }\n" format renderer
          }.mkString
      log.debug("Proguard configuration written to " + proguardConfigurationPath)
      FileUtilities.write(proguardConfigurationPath.asFile, proguardConfiguration, log)
    }
    def packTask = fileTask(outputJpgz from outputJar) {
      Pack.pack(outputJar, outputJp, log) orElse
        FileUtilities.gzip(outputJp, outputJpgz, log)
    }
    def templateMappings = renderInfo match {
      case (width, height, _)  => Map(
        "width" -> width, 
        "height" -> height,
        "sketch" -> name,
        "archive" -> outputJar.asFile.getName
      )
    }
    def writeHtmlTask = templateTask(AppletTemplate.resource, outputHtml)
    
    def appletTask = task {
      try {
        val dsk = Class.forName("java.awt.Desktop")
        dsk.getMethod("browse", classOf[java.net.URI]).invoke(
          dsk.getMethod("getDesktop").invoke(null), outputHtml.asFile.toURI
        )
        None
      } catch {
        case _ => Some("Unable to open browser. Java 5 VM?")
      }
    }
}
