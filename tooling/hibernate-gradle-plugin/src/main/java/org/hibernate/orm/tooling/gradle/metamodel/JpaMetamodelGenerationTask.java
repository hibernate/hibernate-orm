/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle.metamodel;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.inject.Inject;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.orm.tooling.gradle.Helper;
import org.hibernate.orm.tooling.gradle.HibernateOrmSpec;
import org.hibernate.orm.tooling.gradle.metamodel.model.JpaStaticMetamodelGenerator;
import org.hibernate.orm.tooling.gradle.metamodel.model.MetamodelClass;

import static org.hibernate.orm.tooling.gradle.HibernateOrmSpec.HIBERNATE;

/**
 * Generates the "JPA static metamodel" from the domain model defined by the project
 * via classes and possibly XML mappings
 *
 * @apiNote While there is also an annotation-processor that performs the same
 * general function, that approach is limited in that it can only process compiled
 * classes based on annotations.  This task accounts for both classes and XML mappings
 */
public class JpaMetamodelGenerationTask extends DefaultTask {
	public static final String DSL_NAME = "generateJpaMetamodel";
	public static final String COMPILE_DSL_NAME = "compileJpaMetamodel";


	private final HibernateOrmSpec ormSpec;
	private final DirectoryProperty resourcesOutputDir;
	private final SourceSet mainSourceSet;

	@Inject
	@SuppressWarnings( "UnstableApiUsage" )
	public JpaMetamodelGenerationTask(
			HibernateOrmSpec ormSpec,
			SourceSet mainSourceSet,
			JavaCompile mainCompileTask,
			Project project) {
		this.ormSpec = ormSpec;
		dependsOn( mainCompileTask );

		this.mainSourceSet = mainSourceSet;

		final SourceSetOutput mainSourceSetOutput = mainSourceSet.getOutput();

		resourcesOutputDir = project.getObjects().directoryProperty();
		resourcesOutputDir.set( project.getLayout().dir( project.provider( mainSourceSetOutput::getResourcesDir ) ) );

	}

	@InputFiles
	@SkipWhenEmpty
	public FileCollection getJavaClassDirs() {
		return mainSourceSet.getOutput();
	}

	@InputFiles
	@SkipWhenEmpty
	public DirectoryProperty getResourcesOutputDir() {
		// for access to XML mappings
		return resourcesOutputDir;
	}

	@OutputDirectory
	public DirectoryProperty getGenerationOutputDirectory() {
		return ormSpec.getJpaMetamodelSpec().getGenerationOutputDirectory();
	}

	@TaskAction
	public void generateJpaMetamodel() {
		final ClassLoader classLoader = determineUnitClassLoader( getProject(), mainSourceSet );
		final PersistenceUnitInfoImpl unitInfo = new PersistenceUnitInfoImpl(
				determineUnitUrl(),
				generateIntegrationSettings(),
				classLoader
		);

		getJavaClassDirs().forEach(
				classesDir -> {
					final ConfigurableFileTree files = getProject().fileTree( classesDir );
					files.forEach(
							file -> {
								if ( file.getName().endsWith( ".class" ) ) {
									final String className = Helper.determineClassName( classesDir, file );
									unitInfo.addManagedClassName( className );
								}
								else if ( isMappingFile( file ) ) {
									unitInfo.addMappingFile( file.getName() );
								}
							}
					);
				}
		);

		resourcesOutputDir.getAsFileTree().forEach(
				file -> {
					if ( isMappingFile( file ) ) {
						unitInfo.addMappingFile( file.getName() );
					}
				}
		);

		JpaStaticMetamodelGenerator.processMetamodel( unitInfo, ormSpec.getJpaMetamodelSpec() );
	}

	private URL determineUnitUrl() {
		try {
			// NOTE : we just need *a* URL - we used the project dir
			return getProject().getProjectDir().toURI().toURL();
		}
		catch (MalformedURLException e) {
			throw new IllegalStateException( "Could not interpret project directory as URL" );
		}
	}

	@SuppressWarnings( "UnstableApiUsage" )
	private static ClassLoader determineUnitClassLoader(Project project, SourceSet mainSourceSet) {
		final String compileJavaTaskName = mainSourceSet.getCompileJavaTaskName();
		final JavaCompile javaCompileTask = (JavaCompile) project.getTasks().getByName( compileJavaTaskName );
		final URL projectClassesDirUrl = toUrl( javaCompileTask.getDestinationDirectory().get().getAsFile() );

		return new URLClassLoader( new URL[] { projectClassesDirUrl }, MetamodelClass.class.getClassLoader() );
	}

	private static URL toUrl(File file) {
		final URI uri = file.toURI();
		try {
			return uri.toURL();
		}
		catch (MalformedURLException e) {
			throw new GradleException( "Could not convert classpath entry into URL : " + file.getAbsolutePath(), e );
		}
	}

	private Properties generateIntegrationSettings() {
		final Properties settings = new Properties();

		settings.put( "hibernate.temp.use_jdbc_metadata_defaults", "false" );
		settings.put( AvailableSettings.DIALECT, "H2" );
		settings.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, false );
		settings.put( AvailableSettings.USE_QUERY_CACHE, false );

		return settings;
	}

	private boolean isMappingFile(File file) {
		final String fileName = file.getName();

		// convention
		//		- we could allow filters for flexibility?
		return fileName.endsWith( ".hbm.xml" ) || fileName.endsWith( ".orm.xml" );
	}

	private static class PersistenceUnitInfoImpl implements PersistenceUnitInfo {
		private final URL unitRoot;
		private final Properties properties;
		private final ClassLoader classLoader;
		private final List<String> managedClassNames = new ArrayList<>();
		private final List<String> mappingFileNames = new ArrayList<>();

		public PersistenceUnitInfoImpl(URL unitRoot, Properties properties, ClassLoader classLoader) {
			this.unitRoot = unitRoot;
			this.properties = properties;
			this.classLoader = classLoader;
		}

		@Override
		public String getPersistenceUnitName() {
			return "jpa-static-metamodel-gen";
		}

		@Override
		public URL getPersistenceUnitRootUrl() {
			return unitRoot;
		}

		@Override
		public Properties getProperties() {
			return properties;
		}

		@Override
		public ClassLoader getClassLoader() {
			return classLoader;
		}

		@Override
		public List<String> getManagedClassNames() {
			return managedClassNames;
		}

		public void addManagedClassName(String className) {
			getManagedClassNames().add( className );
		}

		@Override
		public List<String> getMappingFileNames() {
			return mappingFileNames;
		}

		public void addMappingFile(String fileName) {
			getMappingFileNames().add( fileName );
		}





		@Override
		public String getPersistenceProviderClassName() {
			return HibernatePersistenceProvider.class.getName();
		}

		@Override
		public PersistenceUnitTransactionType getTransactionType() {
			return null;
		}

		@Override
		public DataSource getJtaDataSource() {
			return null;
		}

		@Override
		public DataSource getNonJtaDataSource() {
			return null;
		}

		@Override
		public List<URL> getJarFileUrls() {
			return null;
		}

		@Override
		public boolean excludeUnlistedClasses() {
			return true;
		}

		@Override
		public SharedCacheMode getSharedCacheMode() {
			return null;
		}

		@Override
		public ValidationMode getValidationMode() {
			return null;
		}

		@Override
		public String getPersistenceXMLSchemaVersion() {
			return null;
		}

		@Override
		public void addTransformer(ClassTransformer transformer) {

		}

		@Override
		public ClassLoader getNewTempClassLoader() {
			return null;
		}
	}

	@SuppressWarnings( "UnstableApiUsage" )
	public static void apply(HibernateOrmSpec pluginDsl, SourceSet mainSourceSet, Project project) {
		final String mainCompileTaskName = mainSourceSet.getCompileJavaTaskName();
		final JavaCompile mainCompileTask = (JavaCompile) project.getTasks().getByName( mainCompileTaskName );
		final Task compileResourcesTask = project.getTasks().getByName( "processResources" );

		final JpaMetamodelGenerationTask genTask = project.getTasks().create(
				DSL_NAME,
				JpaMetamodelGenerationTask.class,
				pluginDsl,
				mainSourceSet,
				mainCompileTask,
				project
		);
		genTask.setGroup( HIBERNATE );
		genTask.setDescription( "Generates the JPA 'static metamodel'" );
		genTask.onlyIf( (t) -> pluginDsl.getSupportJpaMetamodelProperty().getOrElse( true ) );

		genTask.dependsOn( mainCompileTask );
		genTask.dependsOn( compileResourcesTask );

		final JavaCompile compileJpaMetamodelTask = project.getTasks().create( COMPILE_DSL_NAME, JavaCompile.class );
		compileJpaMetamodelTask.setGroup( HIBERNATE );
		compileJpaMetamodelTask.setDescription( "Compiles the JPA static metamodel generated by `" + DSL_NAME + "`" );
		compileJpaMetamodelTask.setSourceCompatibility( mainCompileTask.getSourceCompatibility() );
		compileJpaMetamodelTask.setTargetCompatibility( mainCompileTask.getTargetCompatibility() );
		genTask.finalizedBy( compileJpaMetamodelTask );
		mainCompileTask.finalizedBy( compileJpaMetamodelTask );
		compileJpaMetamodelTask.dependsOn( genTask );
		compileJpaMetamodelTask.source( project.files( pluginDsl.getJpaMetamodelSpec().getGenerationOutputDirectory() ) );
		compileJpaMetamodelTask.getDestinationDirectory().set( pluginDsl.getJpaMetamodelSpec().getCompileOutputDirectory() );
		compileJpaMetamodelTask.setClasspath(
				project.getConfigurations().getByName( "runtimeClasspath" ).plus( mainSourceSet.getRuntimeClasspath() )
		);

		compileJpaMetamodelTask.doFirst(
				(task) -> {
					project.getLogger().lifecycle( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
					project.getLogger().lifecycle( "compileJpaMetamodel classpath" );
					project.getLogger().lifecycle( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
					( (JavaCompile) task ).getClasspath().forEach(
							entry -> project.getLogger().lifecycle( "    > {}", entry.getAbsolutePath() )
					);
					project.getLogger().lifecycle( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
				}
		);
	}

}
