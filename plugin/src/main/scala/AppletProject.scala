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
  val toolsConfig = config("tools")
  val defaultConfig = Configurations.Default
  val proguardJar = "net.sf.proguard" % "proguard" % "4.4" % "tools->default"
  
  /******** Proguard *******/
  private lazy val proguard = proguardTask dependsOn(`package`, glob, writeProguardConfiguration)
  private lazy val writeProguardConfiguration = writeProguardConfigurationTask dependsOn `package`
  private lazy val pack = packTask dependsOn(proguard)
  private lazy val writeHtml = writeHtmlTask dependsOn(glob)
  lazy val applet = (appletTask dependsOn (pack, writeHtml) describedAs
    "Generate an optimized and compressed applet jar and html file in target/applet")
  
  private def proguardTask = fileTask(outputJar from sourceGlob) {
    FileUtilities.clean(outputJar :: Nil, log)
    val proguardClasspathString = Path.makeString(managedClasspath(toolsConfig).get)
    val configFile = proguardConfigurationPath.toString
    val exitValue = Process("java", List("-Xmx256M", "-cp", proguardClasspathString, "proguard.ProGuard", "@" + configFile)) ! log
    if(exitValue == 0) None else Some("Proguard failed with nonzero exit code (" + exitValue + ")")
  }
  object Renderers {
    val P3D = "processing.core.PGraphics3D"
    val JAVA2D = "processing.core.PGraphicsJava2D"
    val OPENGL = "processing.opengl.PGraphicsOpenGL"
    val PDF = "processing.pdf.PGraphicsPDF"
    val DXF = "processing.dxf.RawDXF"
    val map = Map("P3D" -> P3D, "JAVA2D" -> JAVA2D, "OPENGL" -> OPENGL, "PDF" -> PDF, "DXF" -> DXF)
  }
  def renderInfo = {
    // tasks that call here should depend on glob
    val sz = """(?s).*size\s*\(\s*(\d+)\s*,\s*(\d+),?\s*(\S*)\s*\).*""".r
    val sz_line = mainSources.getFiles.projection.elements.flatMap { source =>
      io.Source.fromFile(source).getLines
    } find { None != sz.findFirstIn(_) }
    val univRenderers = Renderers.JAVA2D :: Nil
    
    sz_line match {
      case Some(sz(width, height)) => (width, height, univRenderers)
      case Some(sz(width, height, "")) => (width, height, univRenderers)
      case Some(sz(width, height, r)) => (width, height, Renderers.map(r) :: univRenderers)
      case _ => 
        log.warn("Unable to guess sketch dimensions and renderer in sketch sources. Consider overriding renderInfo to specify.")
        (100, 100, univRenderers)
    }
  }
  def proguardOptions = "-dontobfuscate" :: "-dontoptimize" :: "-dontnote" :: "-dontwarn" :: "-ignorewarnings" :: Nil
  private def writeProguardConfigurationTask = 
    fileTask(proguardConfigurationPath from sourceGlob)
    {
      // the template for the proguard configuration file
      val outTemplate = """
        |%s
        |-outjars %s
        |-keep class %s
        |-keep class spde.core.SApplet { *** scripty(...); }
        |-keepclasseswithmembers class * { public void dispose(); }
        |"""
      
      val defaultJar = (outputPath / defaultJarName).asFile.getAbsolutePath
      log.debug("proguard configuration using main jar " + defaultJar)
      val externalDependencies = Set() ++ (
        mainCompileConditional.analysis.allExternals ++ compileClasspath.get.map { _.asFile }
      ) map { _.getAbsoluteFile } filter {  _.getName.endsWith(".jar") }
      
      def quote(s: Any) = '"' + s.toString + '"'
      log.debug("proguard configuration external dependencies: \n\t" + externalDependencies.mkString("\n\t"))
      // partition jars from the external jar dependencies of this project by whether they are located in the project directory
      // if they are, they are specified with -injars, otherwise they are specified with -libraryjars
      val (projectJars, otherJars) = externalDependencies.toList.partition(jar => Path.relativize(rootProjectDirectory, jar).isDefined)
      // exclude properties files and manifests from scala-library jar
      val inJars = (quote(defaultJar) :: projectJars.map(quote(_) + "(!META-INF/**,!*.txt)")).map("-injars " + _)
      val libraryJars = otherJars.map(quote).map { "-libraryjars " + _ }
      
      val (width, height, renderers) = renderInfo
      
      val proguardConfiguration =
        outTemplate.stripMargin.format(
          (proguardOptions ++ inJars ++ libraryJars).mkString("\n"),
          quote(outputJar.absolutePath),
          appletClass
        ) + renderers.map { renderer =>
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
        "sketch" -> appletClass,
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
