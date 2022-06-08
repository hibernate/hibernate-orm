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
import java.util.Properties;
import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.tooling.gradle.Helper;
import org.hibernate.orm.tooling.gradle.HibernateOrmSpec;
import org.hibernate.orm.tooling.gradle.metamodel.model.GenerationOptions;
import org.hibernate.orm.tooling.gradle.metamodel.model.JpaStaticMetamodelGenerator;

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
	public static final String GEN_TASK_NAME = "generateJpaMetamodel";
	public static final String COMPILE_META_TASK_NAME = "compileJpaMetamodel";

	private final Property<SourceSet> sourceSetProperty;

	private final DirectoryProperty generationOutputDirectory;

	private final Property<Boolean> applyGeneratedAnnotation;
	private final SetProperty<String> suppressions;



	@Inject
	public JpaMetamodelGenerationTask() {
		setGroup( HIBERNATE );
		setDescription( "Generates the JPA 'static metamodel'" );

		sourceSetProperty = getProject().getObjects().property( SourceSet.class );

		generationOutputDirectory = getProject().getObjects().directoryProperty();

		applyGeneratedAnnotation = getProject().getObjects().property( Boolean.class );
		suppressions = getProject().getObjects().setProperty( String.class );

	}

	public void injectSourceSet(Provider<SourceSet> sourceSetAccess) {
		sourceSetProperty.set( sourceSetAccess );
	}

	@OutputDirectory
	public DirectoryProperty getGenerationOutputDirectory() {
		return generationOutputDirectory;
	}

	@InputFiles
	@SkipWhenEmpty
	public FileCollection getSources() {
		return sourceSetProperty.get().getOutput();
	}

	@Input
	public Property<Boolean> getApplyGeneratedAnnotation() {
		return applyGeneratedAnnotation;
	}

	@Input
	public SetProperty<String> getSuppressions() {
		return suppressions;
	}

	@TaskAction
	public void generateJpaMetamodel() {
		final ClassLoader classLoader = Helper.toClassLoader( sourceSetProperty.get().getOutput() );
		final PersistenceUnitInfoImpl unitInfo = new PersistenceUnitInfoImpl(
				determineUnitUrl(),
				generateIntegrationSettings(),
				classLoader
		);


		getSources().forEach( (dir) -> {
			final ConfigurableFileTree files = getProject().fileTree( dir );
			files.forEach( (file) -> {
				if ( file.getName().endsWith( ".class" ) ) {
					final String className = Helper.determineClassName( dir, file );
					unitInfo.addManagedClassName( className );
				}
				else if ( isMappingFile( file ) ) {
					unitInfo.addMappingFile( file.getName() );
				}
			} );
		} );

		JpaStaticMetamodelGenerator.processMetamodel( unitInfo, createGenerationOptions() );
	}

	private GenerationOptions createGenerationOptions() {
		return new GenerationOptions() {
			@Override
			public Provider<Directory> getGenerationDirectory() {
				return generationOutputDirectory;
			}

			@Override
			public Provider<Boolean> getApplyGeneratedAnnotation() {
				return applyGeneratedAnnotation;
			}

			@Override
			public SetProperty<String> getSuppressions() {
				return suppressions;
			}
		};
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

	public static void apply(HibernateOrmSpec pluginDsl, Project project) {
		// todo : implement it
	}

}
