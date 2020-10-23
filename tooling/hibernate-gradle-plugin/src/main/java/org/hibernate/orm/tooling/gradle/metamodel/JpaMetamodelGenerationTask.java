/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle.metamodel;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.inject.Inject;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.TaskAction;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.orm.tooling.gradle.Helper;
import org.hibernate.orm.tooling.gradle.HibernateOrmSpec;
import org.hibernate.orm.tooling.gradle.metamodel.model.MetamodelClass;
import org.hibernate.orm.tooling.gradle.metamodel.model.ObjectFactory;

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

	public static void apply(HibernateOrmSpec ormDsl, SourceSet mainSourceSet, Project project) {
		final JpaMetamodelGenerationTask genTask = project.getTasks().create( DSL_NAME, JpaMetamodelGenerationTask.class, ormDsl, mainSourceSet, project );
		genTask.setGroup( HIBERNATE );
		genTask.setDescription( "Generates the JPA 'static metamodel'" );

		final String compileJavaTaskName = mainSourceSet.getCompileJavaTaskName();
		final Task compileJavaTask = project.getTasks().getByName( compileJavaTaskName );
		genTask.dependsOn( compileJavaTask );

		final Task compileResourcesTask = project.getTasks().getByName( "processResources" );
		genTask.dependsOn( compileResourcesTask );
	}

	private final FileCollection javaClassDirs;
	private final DirectoryProperty resourcesOutputDir;
	private final DirectoryProperty outputDirectory;

	@Inject
	@SuppressWarnings( "UnstableApiUsage" )
	public JpaMetamodelGenerationTask(HibernateOrmSpec ormDsl, SourceSet mainSourceSet, Project project) {
		final SourceSetOutput mainSourceSetOutput = mainSourceSet.getOutput();

		javaClassDirs = mainSourceSetOutput.getClassesDirs();

		resourcesOutputDir = project.getObjects().directoryProperty();
		final ProjectLayout projectLayout = project.getLayout();
		resourcesOutputDir.set( projectLayout.dir( project.provider( mainSourceSetOutput::getResourcesDir ) ) );

		outputDirectory = ormDsl.getJpaMetamodelSpec().getOutputDirectory();
	}

	@InputFiles
	@SkipWhenEmpty
	public FileCollection getJavaClassDirs() {
		return javaClassDirs;
	}

	@InputFiles
	@SkipWhenEmpty
	public DirectoryProperty getResourcesOutputDir() {
		// for access to XML mappings
		return resourcesOutputDir;
	}

	@OutputDirectory
	public DirectoryProperty getOutputDirectory() {
		return outputDirectory;
	}

	@TaskAction
	public void generateJpaMetamodel() {
		// todo : need to generate a compile task for these generated sources

		final Properties integrationSettings = generateIntegrationSettings();

		final URL unitUrl = determineUnitUrl();
		final ClassLoader classLoader = Helper.toClassLoader( javaClassDirs );
		final PersistenceUnitInfoImpl unitInfo = new PersistenceUnitInfoImpl( unitUrl, classLoader );

		javaClassDirs.forEach(
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

		final EntityManagerFactoryBuilder emfb = Bootstrap.getEntityManagerFactoryBuilder( unitInfo, integrationSettings );

		try {
			generateMetamodel( emfb );
		}
		finally {
			emfb.cancel();
		}
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

	private void generateMetamodel(EntityManagerFactoryBuilder emfb) {
		final MetadataImplementor metadata = emfb.metadata();
		final Set<MappedSuperclass> mappedSuperclasses = metadata.getMappedSuperclassMappingsCopy();
		if ( mappedSuperclasses != null ) {
			mappedSuperclasses.forEach( this::handleMappedClass );
		}

		final java.util.Collection<PersistentClass> entityBindings = metadata.getEntityBindings();
		if ( entityBindings != null ) {
			entityBindings.forEach( this::handlePersistentClass );
		}
	}

	private Properties generateIntegrationSettings() {
		final Properties settings = new Properties();

		// see `JdbcEnvironmentInitiator#initiateService`
		settings.put( "hibernate.temp.use_jdbc_metadata_defaults", "false" );

		settings.put( AvailableSettings.DIALECT, "H2" );

		return settings;
	}

	@SuppressWarnings( "unchecked" )
	private void handleMappedClass(MappedSuperclass mappingDescriptor) {
		final MetamodelClass metamodelClass = ObjectFactory.metamodelClass( mappingDescriptor );
		handleManagedClass( metamodelClass, mappingDescriptor.getDeclaredPropertyIterator() );
	}

	@SuppressWarnings( "unchecked" )
	private void handlePersistentClass(PersistentClass persistentClass) {
		final MetamodelClass metamodelClass = ObjectFactory.metamodelClass( persistentClass );
		handleManagedClass( metamodelClass, persistentClass.getDeclaredPropertyIterator() );
	}

	private void handleManagedClass(MetamodelClass metamodelClass, Iterator<Property> propertyIterator) {
		propertyIterator.forEachRemaining(
				property -> metamodelClass.addAttribute( ObjectFactory.attribute( property, property.getValue(), metamodelClass ) )
		);

		final String replaced = metamodelClass.getMetamodelClassName().replace( '.', '/' );
		final String metamodelClassFileName = replaced + ".class";
		final RegularFile metamodelClassFile = outputDirectory.file( metamodelClassFileName ).get();

		final File metamodelClassFileAsFile = metamodelClassFile.getAsFile();
		metamodelClassFileAsFile.mkdirs();
		metamodelClass.writeToFile( metamodelClassFileAsFile );
	}

	private boolean isMappingFile(File file) {
		final String fileName = file.getName();

		// convention
		//		- we could allow filters for flexibility?
		return fileName.endsWith( ".hbm.xml" ) || fileName.endsWith( ".orm.xml" );
	}

	private static class PersistenceUnitInfoImpl implements PersistenceUnitInfo {
		private final URL unitRoot;
		private final ClassLoader classLoader;
		private final List<String> managedClassNames = new ArrayList<>();
		private final List<String> mappingFileNames = new ArrayList<>();

		public PersistenceUnitInfoImpl(URL unitRoot, ClassLoader classLoader) {
			this.unitRoot = unitRoot;
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
		public ClassLoader getClassLoader() {
			return classLoader;
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
		public Properties getProperties() {
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


}
