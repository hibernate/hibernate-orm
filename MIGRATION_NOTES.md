# Migration Notes

Curated false-positive analysis of the `apply_migrations.py` deferrals. Every entry below was
checked against the receiver type at the call site (the receiver-type ladder in task 06) and
confirmed **not** to be a migrated Gradle property — the scanner matched a bare getter *name*
against a Gradle class that merely declares a same-named lazy property, but the actual receiver is
a JDK type, an `org.gradle.api.Project`, a container, a worker `WorkParameters`, or a Hibernate
domain object.

The real sites the scanner found have already been rewritten and removed from this file:
- the seven Cat-C operator mutations (`compilerArgs +=`, `jvmArgs +=`, `systemProperties +=`) in
  `hibernate-core.gradle`, `local.java-module.gradle`, and `hibernate-processor.gradle`;
- the one `[CONFIRMED: BaseForkOptions]` Cat-B read `forOptions.getJvmArgs()` in `JavaModulePlugin`.

## Confirmed false positives

### `local-build-plugins/src/main/java/org/hibernate/build/OrmBuildDetails.java`
- line 30 — `project` is org.gradle.api.Project; Project.getRootDir() returns a File and is not lazy-migrated (matched VCS types it does not implement)
  - `versionFileAccess = project.provider( () -> new File( project.getRootDir(), HibernateVersion.RELATIVE_FILE ) );`

### `local-build-plugins/src/main/java/org/hibernate/build/aspects/ModuleAspect.java`
- line 37 — `target` is a Project; getConfigurations() returns the ConfigurationContainer, a container that is never lazy-migrated
  - `target.getConfigurations().configureEach( (files) -> {`

### `local-build-plugins/src/main/java/org/hibernate/build/maven/embedder/MavenEmbedderPlugin.java`
- line 46 — `spec` is a Gradle worker spec; getParameters() returns the action's WorkParameters, not GroovyCompileOptions
  - `spec.getParameters().getProjectVersion().set( project.getVersion().toString() );`
- line 46 — `project.getVersion()` seeds the worker projectVersion parameter; Project.getVersion() is not lazy-migrated
  - `spec.getParameters().getProjectVersion().set( project.getVersion().toString() );`
- line 47 — `spec.getParameters()` again returns the worker WorkParameters (workingDirectory wiring)
  - `spec.getParameters().getWorkingDirectory().set( workingDirectory );`
- line 48 — `spec.getParameters()` again returns the worker WorkParameters (mavenLocalDirectory wiring)
  - `spec.getParameters().getMavenLocalDirectory().set( dsl.getLocalRepositoryDirectory() );`
- line 175 — `project.getVersion()` builds the hibernate-maven-plugin jar name; Project.getVersion()
  - `final String artifactName = "hibernate-maven-plugin-" + project.getVersion() + ".jar";`
- line 201 — `task.getProject().getVersion()` builds a -Dversion argument; Project.getVersion()
  - `arguments.add("-Dversion=" + task.getProject().getVersion());`
- line 208 — `project.getVersion()` builds the hibernate-core jar name; Project.getVersion()
  - `final String artifactName = "hibernate-core-" + project.getVersion() + ".jar";`
- line 223 — `project.getVersion()` builds the hibernate-scan-jandex jar name; Project.getVersion()
  - `final String artifactName = "hibernate-scan-jandex-" + project.getVersion() + ".jar";`

### `local-build-plugins/src/main/java/org/hibernate/build/maven/embedder/MavenEmbedderService.java`
- line 36 — `getParameters()` is this WorkAction's own WorkParameters (multiModuleProjectDirectory), not GroovyCompileOptions
  - `System.setProperty( "maven.multiModuleProjectDirectory", getParameters().getWorkingDirectory().toString() );`
- line 44 — `getParameters()` WorkAction WorkParameters (mavenLocalDirectory read)
  - `final Directory mavenLocalDirectory = getParameters().getMavenLocalDirectory().get();`
- line 46 — `getParameters()` WorkAction WorkParameters (projectVersion read)
  - `cml.add( "-Dorm.project.version=" + getParameters().getProjectVersion().get() );`
- line 48 — `getParameters()` WorkAction WorkParameters (workingDirectory read)
  - `final Directory workingDirectory = getParameters().getWorkingDirectory().get();`

### `local-build-plugins/src/main/java/org/hibernate/build/maven/embedder/MavenPluginDescriptorTask.java`
- line 30 — `getProject().getVersion()` registered as a task input value; Project.getVersion()
  - `getInputs().property( "project-version", getProject().getVersion() );`

### `local-build-plugins/src/main/java/org/hibernate/build/xjc/SchemaDescriptorFactory.java`
- line 68 — `schemaDescriptor` is a Hibernate SchemaDescriptor; getName() is a domain accessor (first letter)
  - `final char initialLetterCap = Character.toUpperCase( schemaDescriptor.getName().charAt( 0 ) );`
- line 69 — `schemaDescriptor.getName()` Hibernate SchemaDescriptor domain accessor (remaining substring)
  - `final String rest = schemaDescriptor.getName().substring( 1 );`

### `local-build-plugins/src/main/java/org/hibernate/build/xjc/XjcTask.java`
- line 51 — `regularFile.getAsFile()` is a java.io.File; File.getName() is JDK API
  - `schemaName.convention( xsdFile.map( regularFile -> regularFile.getAsFile().getName() ) );`

### `local-build-plugins/src/main/java/org/hibernate/orm/antlr/AntlrHelper.java`
- line 59 — `generatedJavaFile` is a java.io.File; File.getName() is JDK API
  - `final File outputFile = new File( outputDirectory, generatedJavaFile.getName() );`

### `local-build-plugins/src/main/java/org/hibernate/orm/antlr/AntlrPlugin.java`
- line 47 — `project.getConfigurations().maybeCreate(...)` operates on the ConfigurationContainer; container, not lazy-migrated
  - `final Configuration antlrDependencies = project.getConfigurations().maybeCreate( ANTLR );`
- line 51 — `getSourceSets()` chained off the JavaPluginExtension returns a SourceSetContainer; container, not lazy-migrated
  - `.getSourceSets()`

### `local-build-plugins/src/main/java/org/hibernate/orm/antlr/SplitGrammarGenerationTask.java`
- line 74 — `getProject().getConfigurations().getByName(...)` operates on the ConfigurationContainer; container
  - `antlrConfiguration = getProject().getConfigurations().getByName( "antlr" );`
- line 120 — `grammarDescriptor` is a Hibernate grammar descriptor; getName() domain accessor (first arg)
  - `grammarDescriptor.getName(),`
- line 144 — `grammarDescriptor.getName()` Hibernate grammar descriptor domain accessor (second call site)
  - `grammarDescriptor.getName(),`

### `local-build-plugins/src/main/java/org/hibernate/orm/post/AbstractJandexAwareTask.java`
- line 137 — `inclusion` is a Hibernate Inclusion; getPath() domain accessor (startsWith guard)
  - `if ( previousPath != null && inclusion.getPath().startsWith( previousPath ) ) {`
- line 143 — `inclusion.getPath()` Hibernate Inclusion domain accessor (written to report)
  - `fileWriter.write( inclusion.getPath() );`
- line 151 — `inclusion.getPath()` Hibernate Inclusion domain accessor (in exception message)
  - `throw new RuntimeException( "Error writing entry (" + inclusion.getPath() + ") to report file", e );`
- line 154 — `inclusion.getPath()` Hibernate Inclusion domain accessor (previousPath assignment)
  - `previousPath = inclusion.getPath();`

### `local-build-plugins/src/main/java/org/hibernate/orm/post/DeprecationReportTask.java`
- line 25 — `Deprecated.class.getName()` is java.lang.Class.getName(); JDK reflection API
  - `public static final String DEPRECATED_ANN_NAME = Deprecated.class.getName();`

### `local-build-plugins/src/main/java/org/hibernate/orm/post/DialectReportTask.java`
- line 241 — `dialectImplClass.getName()` is java.lang.Class.getName(); JDK reflection API
  - `throw new RuntimeException( "Unable to create DialectDelegate for " + dialectImplClass.getName(), e );`
- line 267 — `loadedDialectClass.getName()` is java.lang.Class.getName(); JDK reflection API
  - `throw new RuntimeException( "Unable to access " + DialectClassDelegate.MIN_VERSION_METHOD_NAME + " for " + dialectClassDelegate.loadedDialectClass.getName(), e );`

### `local-build-plugins/src/main/java/org/hibernate/orm/post/IndexManager.java`
- line 169 — `details` is an IndexManager class-file detail; getFile() opens its stream (domain accessor)
  - `try (final FileInputStream stream = new FileInputStream( details.getFile() )) {`
- line 173 — `details.getFile()` IndexManager detail domain accessor (indexing-problem lifecycle log)
  - `.lifecycle( "Problem indexing class file - " + details.getFile()`
- line 178 — `details.getFile()` IndexManager detail domain accessor (indexing-problem exception)
  - `throw new RuntimeException( "Problem indexing class file - " + details.getFile()`
- line 182 — `details.getFile()` IndexManager detail domain accessor (locating-problem exception)
  - `throw new RuntimeException( "Problem locating project class file - " + details.getFile()`
- line 186 — `details.getFile()` IndexManager detail domain accessor (access-error exception)
  - `throw new RuntimeException( "Error accessing project class file - " + details.getFile()`
- line 221 — `inclusion.getPath()` is a Hibernate Inclusion domain accessor (written to report)
  - `fileWriter.write( inclusion.getPath() );`

### `local-build-plugins/src/main/java/org/hibernate/orm/post/LoggingReportTask.java`
- line 171 — `subSystem` is a Hibernate SubSystem; getName() domain accessor (anchor heading)
  - `fileWriter.write( "`" + subSystem.getName() + "`::\n" );`
- line 173 — `subSystem.getDescription()` is a Hibernate SubSystem domain accessor
  - `fileWriter.write( "    * Description = " + subSystem.getDescription() + "\n" );`
- line 206 — `subSystem.getName()` Hibernate SubSystem domain accessor (table cell)
  - `subSystem.getName()`

### `local-build-plugins/src/main/java/org/hibernate/orm/post/ReportGenerationPlugin.java`
- line 22 — `project.getConfigurations()` for `artifactsToProcess`; ConfigurationContainer, not lazy-migrated
  - `final Configuration artifactsToProcess = project.getConfigurations()`
- line 58 — `project.getConfigurations()` for `dialectConfig`; ConfigurationContainer
  - `final var dialectConfig = project.getConfigurations()`
- line 72 — `project.getConfigurations()` for `communityDialectConfig`; ConfigurationContainer
  - `final var communityDialectConfig = project.getConfigurations()`

### `local-build-plugins/src/main/java/org/hibernate/orm/properties/AsciiDocWriter.java`
- line 55 — `sectionDescriptor` is a Hibernate SectionDescriptor; getName() domain accessor
  - `final String sectionName = sectionDescriptor.getName();`
- line 65 — `settingDescriptor` is a Hibernate SettingDescriptor; getName() domain accessor (anchor)
  - `tryToWriteLine( writer, "[[", anchorNameBase, "-", settingDescriptor.getName(), "]]" );`
- line 70 — `settingDescriptor.getComment()` is a Hibernate SettingDescriptor domain accessor
  - `writer.write( settingDescriptor.getComment() );`
- line 134 — `settingDescriptor.getName()` Hibernate SettingDescriptor domain accessor (cell)
  - `settingDescriptor.getName()`
- line 141 — `settingDescriptor.getName()` Hibernate SettingDescriptor domain accessor (second cell)
  - `settingDescriptor.getName()`

### `local-build-plugins/src/main/java/org/hibernate/orm/properties/SettingsDocGenerationTask.java`
- line 61 — `dslExtension` is the project's own SettingsDocExtension; getOutputFile() is its own RegularFileProperty, already wired via convention()
  - `outputFile.convention( dslExtension.getOutputFile() );`

### `local-build-plugins/src/main/java/org/hibernate/orm/toolchains/JavaModulePlugin.java`
- line 53 — `javaPluginExtension.getSourceSets()` returns a SourceSetContainer; container, not lazy-migrated
  - `final SourceSetContainer sourceSets = javaPluginExtension.getSourceSets();`
- line 72 — `compileTask.getName()` is org.gradle.api.Task.getName(); Task-only, not lazy-migrated
  - `if ( compileTask.getName().equals( mainSourceSet.getCompileJavaTaskName() ) ) {`

### `tooling/hibernate-gradle-plugin/hibernate-gradle-plugin.gradle`
- line 144 — bare `getVersion()` resolves to project.version (Project.getVersion()); registered as a task input
  - `inputs.property( "orm-version", getVersion() )`
- line 146 — bare `getVersion()` resolves to project.version (Project.getVersion()); ReplaceTokens value
  - `filter( ReplaceTokens, tokens: [ 'hibernateVersion': getVersion() ] )`

### `tooling/hibernate-gradle-plugin/src/functionalTest/java/org/hibernate/orm/tooling/gradle/reveng/TestTemplate.java`
- line 71 — `buildResult` is a TestKit BuildResult; getOutput() returns its String build log, not JacocoTaskExtension.output
  - `assertTrue(buildResult.getOutput().contains("BUILD SUCCESSFUL"));`

### `tooling/hibernate-gradle-plugin/src/main/java/org/hibernate/orm/tooling/gradle/HibernateOrmPlugin.java`
- line 46 — `project.getPath()` is org.gradle.api.Project.getPath(); not lazy-migrated
  - `project.getLogger().debug( "Adding Hibernate extensions to the build [{}]", project.getPath() );`
- line 59 — `getSourceSets()` chained off the JavaPluginExtension returns a SourceSetContainer; container
  - `.getSourceSets()`
- line 63 — `sourceSet.getName()` is org.gradle.api.tasks.SourceSet.getName(); not lazy-migrated
  - `(use && sourceSetName.equals( sourceSet.getName() ))`
- line 86 — `task.getName()` is org.gradle.api.Task.getName(); Task-only
  - `.matching( task -> task.getName().equals( languageCompileTaskName ) )`
- line 88 — `sourceSet.getOutput()` returns a SourceSetOutput, not JacocoTaskExtension.output
  - `FileCollection classesDirs = sourceSet.getOutput().getClassesDirs();`
- line 90 — `getConfigurations()` chained off the project returns a ConfigurationContainer; container
  - `.getConfigurations()`
- line 115 — `javaPluginExtension.getSourceSets().getByName(...)` operates on a SourceSetContainer; container
  - `return javaPluginExtension.getSourceSets().getByName( name );`

### `tooling/hibernate-gradle-plugin/src/main/java/org/hibernate/orm/tooling/gradle/enhance/EnhancementHelper.java`
- line 70 — `subLocation` is a java.io.File; File.getName() is JDK API
  - `else if ( subLocation.isFile() && subLocation.getName().endsWith( ".class" ) ) {`
- line 90 — `subLocation.getName()` java.io.File JDK API (second enhancement loop)
  - `else if ( subLocation.isFile() && subLocation.getName().endsWith( ".class" ) ) {`

### `tooling/hibernate-gradle-plugin/src/main/java/org/hibernate/orm/tooling/gradle/misc/TransformHbmXmlTask.java`
- line 122 — `getSource()` is SourceTask.getSource() returning a FileTree; absent from the migration data, so not lazy-migrated
  - `getSource().forEach( (hbmXmlFile) -> {`
- line 210 — `hbmXmlFile` is a java.io.File; File.getName() is JDK API
  - `final String hbmXmlFileName = hbmXmlFile.getName();`

### `tooling/hibernate-gradle-plugin/src/main/java/org/hibernate/orm/tooling/gradle/reveng/GenerateJavaTask.java`
- line 27 — `pojoExporter.getProperties()` returns a java.util.Properties; setProperty() is the JDK API, not WriteProperties.properties
  - `pojoExporter.getProperties().setProperty("ejb3", String.valueOf(getRevengSpec().generateAnnotations));`
- line 28 — `pojoExporter.getProperties().setProperty(...)` java.util.Properties JDK API (jdk5 flag)
  - `pojoExporter.getProperties().setProperty("jdk5", String.valueOf(getRevengSpec().useGenerics));`

### `tooling/hibernate-gradle-plugin/src/main/java/org/hibernate/orm/tooling/gradle/reveng/RevengTask.java`
- line 53 — bare `getName()` is this task's own org.gradle.api.Task.getName(); Task-only (start log)
  - `getLogger().lifecycle("Starting Task '" + getName() + "'");`
- line 64 — bare `getName()` this task's own Task.getName(); Task-only (end log)
  - `getLogger().lifecycle("Ending Task '" + getName() + "'");`
- line 70 — `getProject().getConfigurations()` returns a ConfigurationContainer; container
  - `ConfigurationContainer cc = getProject().getConfigurations();`
- line 77 — `resolvedArtifacts[i].getFile()` is ResolvedArtifact.getFile() returning a File; not lazy-migrated
  - `urls[i] = resolvedArtifacts[i].getFile().toURI().toURL();`
- line 125 — `f.getName()` is java.io.File.getName(); JDK API
  - `if (filename.equals(f.getName())) {`
- line 146 — `propertyFile.getPath()` is java.io.File.getPath(); JDK API
  - `getLogger().lifecycle("Loading the properties file : " + propertyFile.getPath());`
</content>
</invoke>
