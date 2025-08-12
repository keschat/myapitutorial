01:29:39.134] [WARN ] [com.myapitutorial.server.Main.main()] o.e.j.e.s.ServletContextHandler - BaseResource file:///home/kesi/workspace/java/yt/apiburn/myapitutorial/myapitutorial-server/src/main/resources/www/ is aliased to file:///home/kesi/workspace/java/yt/apiburn/myapitutorial/myapitutorial-server/src/main/resources/www/ in oeje10s.ServletContextHandler@1e54da74{ROOT,/,b=file:///home/kesi/workspace/java/yt/apiburn/myapitutorial/myapitutorial-server/src/main/resources/www/,a=STOPPED,h=oeje10s.ServletHandler@2f2324ad{STOPPED}}. May not be supported in future releases.


The warning you're seeing is related to how Jetty handles the base resource path for serving static files. The issue is with this line:

'''java
apiContext.setBaseResource(apiContext.newResource("myapitutorial-server/src/main/resources/www"));
'''

The warning occurs because Jetty detects that the resource path might be aliased, which could cause issues in future releases. Here's the minimal fix:

Fix the base resource path to use a proper resource URL to eliminate the aliasing warning

'''java
apiContext.setBaseResource(apiContext.newResource(Main.class.getClassLoader().getResource("www")));
'''

This change uses the class loader to properly resolve the www resource directory, which eliminates the aliasing warning by using the correct resource URL instead of a file path that Jetty interprets as potentially aliased.


-----


When using WebAppContext, the base resource is often automatically derived from the setWar parameter, as it typically points to a WAR file or a directory containing the web application's content. However, for ServletContextHandler or ResourceHandler, you need to explicitly set the base resource to enable serving static files.



------

[04:31:30.819] [WARN ] [qtp2013901650-55] o.e.j.ee10.servlet.ServletChannel - /api
java.lang.NoClassDefFoundError: org/glassfish/jersey/innate/inject/InternalBinder

Context
The issue is a version mismatch between Jersey versions. You have Jersey 4.0.0-M2 in some places and 3.1.11 in others. The InternalBinder class location changed between versions. Let me fix this by making all Jersey dependencies use the same version:


------

WARNING: A class jakarta.activation.DataSource for a default provider MessageBodyWriter<jakarta.activation.DataSource> was not found. The provider is not available.
Aug 12, 2025 4:33:08 AM org.glassfish.jersey.server.wadl.WadlFeature configure
WARNING: JAX-B API not found . WADL feature is disabled.

Context

These are just warnings and won't prevent your application from working, but if you want to eliminate them, you need to add the missing Jakarta Activation and JAXB dependencies. Here's the minimal fix:

Add missing Jakarta Activation and JAXB dependencies to eliminate warnings

            <dependency>
                <groupId>jakarta.activation</groupId>
                <artifactId>jakarta.activation-api</artifactId>
                <version>2.1.3</version>
            </dependency>
            <dependency>
                <groupId>jakarta.xml.bind</groupId>
                <artifactId>jakarta.xml.bind-api</artifactId>
                <version>4.0.2</version>
            </dependency>
            
These dependencies provide:

jakarta.activation-api: Resolves the DataSource MessageBodyWriter warning

jakarta.xml.bind-api: Enables WADL feature support

The warnings were appearing because Jersey was looking for these optional features but couldn't find the required classes on the classpath.


------

WARNING: JAXBContext implementation could not be found. WADL feature is disabled.

        <dependency>
                <groupId>org.glassfish.jaxb</groupId>
                <artifactId>jaxb-runtime</artifactId>
                <version>4.0.5</version>
            </dependency>