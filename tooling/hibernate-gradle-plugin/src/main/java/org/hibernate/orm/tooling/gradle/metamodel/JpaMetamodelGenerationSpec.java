/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle.metamodel;

import java.util.Arrays;
import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

import org.hibernate.orm.tooling.gradle.HibernateOrmSpec;

/**
 * @author Steve Ebersole
 */
public class JpaMetamodelGenerationSpec {
	public static final String JPA_METAMODEL = "jpaMetamodel";
	public static final String DSL_NAME = JPA_METAMODEL;

	private final Project project;

	private final DirectoryProperty generationOutputDirectory;
	private final DirectoryProperty compileOutputDirectory;

	private final Property<Boolean> applyGeneratedAnnotation;
	private final SetProperty<String> suppressions;

	@Inject
	public JpaMetamodelGenerationSpec(HibernateOrmSpec ormDsl, Project project) {
		this.project = project;

		generationOutputDirectory = project.getObjects().directoryProperty();
		generationOutputDirectory.convention(
				project.getLayout().getBuildDirectory().dir( "generated/sources/" + JPA_METAMODEL )
		);

		compileOutputDirectory = project.getObjects().directoryProperty();
		compileOutputDirectory.convention(
				project.getLayout().getBuildDirectory().dir( "classes/java/" + JPA_METAMODEL )
		);

		applyGeneratedAnnotation = project.getObjects().property( Boolean.class );
		applyGeneratedAnnotation.convention( true );

		suppressions = project.getObjects().setProperty( String.class );
		suppressions.convention( Arrays.asList( "raw", "deprecation" ) );
	}

	public Property<Boolean> getApplyGeneratedAnnotation() {
		return applyGeneratedAnnotation;
	}

	public SetProperty<String> getSuppressions() {
		return suppressions;
	}

	public DirectoryProperty getGenerationOutputDirectory() {
		return generationOutputDirectory;
	}

	public DirectoryProperty getCompileOutputDirectory() {
		return compileOutputDirectory;
	}


	/**
	 * @see #getApplyGeneratedAnnotation()
	 *
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void applyGeneratedAnnotation(boolean apply) {
		applyGeneratedAnnotation.set( apply );
	}

	/**
	 * @see #getSuppressions()
	 *
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void suppress(String warning) {
		suppressions.add( warning );
	}

	/**
	 * @see #getGenerationOutputDirectory()
	 *
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void generationOutputDirectory(Object ref) {
		generationOutputDirectory.set( project.file( ref ) );
	}

	/**
	 * @see #getCompileOutputDirectory()
	 *
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void compileOutputDirectory(Object ref) {
		compileOutputDirectory.set( project.file( ref ) );
	}
}
