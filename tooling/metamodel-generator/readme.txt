                          JPA Model Generator

  What is it?
  -----------

  This is a Java 6 annotation processor generating meta model classes for the JPA 2 criteria queries.
  The processor (JPAMetaModelEntityProcessor) processes all classes annotated with @Entity, as well as
  entities mapped in /META-INF/orm.xml and mapping files specified in persistence.xml.


  Status
  ------

  This is an alpha release of the annotation processor. The implemented functionality includes:
   - full support for annotations honoring the access type (v2.0) 
   - support for persistence.xml, orm.xml and  <mapping-file>
   - tests (both via compilation failure and regular assertion failure)


  System Requirements
  -------------------

  JDK 1.6 or above.
 

  Issues
  ------

  See issues.txt


  Using JPA Model Generator
  -------------------------

  - Copy jpamodelgen-*.jar together will all jar files from lib into the  classpath of your application. 
    The jpamodelgen jar file contains a service file (/META-INF/services/javax.annotation.processing.Processor) 
    so that the annotation processor will automatically be executed during compilation. 
    You can also explicitly specify the processor using the -processor flag:
    > javac -cp <myclasspath> -d <target> -sourcepath <sources> -processor org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor

  
  * Maven
  This distribution contains a pom.xml file showing one of three possible ways to integrate the processor in a maven project. 
  You can just add <processor>org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor</processor> to the maven-compiler-plugin.
  This approach has, however, the shortcoming that messages from the annotation processor are not displayed. This is a known
  issue. See also - http://weblogs.java.net/blog/ss141213/archive/2007/11/my_maven_experi.html
  The second alternative is the maven-annotation-plugin (http://code.google.com/p/maven-annotation-plugin/). This approach
  hasn't been tested yet. 
  Last but not least, you can use the maven-antrun-plugin to just run the annotation processor and ignore the processor in 
  in the maven-compiler-plugin via '-proc:none'. This is the approach chosen in the POM for this project.

  * Ant
  Make sure the annotation processor and its dependencies are in the classpath. Due the service file the processor will be
  automatically executed when the javac task executes.
  If not try adding <compilerarg value="-processor org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor"/>

  * Idea
  Again, if in the classpath the JPAMetaModelEntityProcessor should execute automatically. If not add the following under 
  'Compiler->Java Compiler': -target 1.6 -processor org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor
  You can also turn of annotation processing via: -target 1.6 -proc:none


  


