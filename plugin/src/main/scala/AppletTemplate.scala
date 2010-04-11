package spde

object AppletTemplate {
  // Hey, maybe there is a way to get resources in a sbt plugin, but for now...
  val resource = 
"""
<html>

<!--  IF and only if the web server is configured to serve jpgz files with 
      the correct content type and encoding, you can swap the 3 jar names
      below to their smaller jpgz equivalents. e.g., for Apache:
    
      AddEncoding pack200-gzip .jpgz
      AddType application/x-java-archive .jpgz -->

<!-- This version plays nicer with older browsers, 
     but requires JavaScript to be enabled. 
     http://java.sun.com/javase/6/docs/technotes/guides/jweb/deployment_advice.html -->

<script type="text/javascript"
  src="http://www.java.com/js/deployJava.js"></script>
<script type="text/javascript">
  /* <![CDATA[ */

  var attributes = { code:'{{sketch}}.class',
                           archive: '{{archive}}',
                           width:{{width}}, height:{{height}} } ;
        var parameters = { };
        var version = '1.5';
        deployJava.runApplet(attributes, parameters, version);

        /* ]]> */
      </script>
<div>
<noscript> <div>
  <!--[if !IE]> -->
  <object classid="java:{{sketch}}.class" 
              type="application/x-java-applet"
              archive="{{archive}}"
              width="{{width}}" height="{{height}}"
              standby="Loading Processing software..." >
          
    <param name="archive" value="{{archive}}" />
    
    <param name="mayscript" value="true" />
    <param name="scriptable" value="true" />
    
    <param name="boxmessage" value="Loading Processing software..." />
    <param name="boxbgcolor" value="#FFFFFF" />
    
    <param name="test_string" value="outer" />
  <!--<![endif]-->

    <object classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93"
      codebase="http://java.sun.com/update/1.6.0/jinstall-6u18-windows-i586.cab"
      width="{{width}}" height="{{height}}"
      standby="Loading Processing software..."  >
      
      <param name="code" value="{{sketch}}.class" />
      <param name="archive" value="{{archive}}" />
      
      <param name="mayscript" value="true" />
      <param name="scriptable" value="true" />
      
      <param name="boxmessage" value="Loading Processing software..." />
      <param name="boxbgcolor" value="#FFFFFF" />
      
      <param name="test_string" value="inner" />
      
      <p>
  <strong>
    This browser does not have a Java Plug-in.
    <br />
    <a href="http://www.java.com/getjava" title="Download Java Plug-in">
      Get the latest Java Plug-in here.
    </a>
  </strong>
      </p>
      
    </object>
    
  <!--[if !IE]> -->
  </object>
  <!--<![endif]-->

</div> </noscript>
</div>

</html>
"""
}