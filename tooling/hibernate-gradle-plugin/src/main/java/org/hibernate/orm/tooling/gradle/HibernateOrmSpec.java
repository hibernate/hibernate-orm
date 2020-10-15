/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.util.ConfigureUtil;

import groovy.lang.Closure;

/**
 * Main DSL extension for Hibernate ORM.  Available as `project.hibernateOrm`
 */
public abstract class HibernateOrmSpec implements ExtensionAware {
	public static final String HIBERNATE = "hibernate";
	public static final String ORM = "orm";
	public static final String DEFAULT_OUTPUT_PATH = HIBERNATE + "/" + ORM;

	public static final String DSL_NAME = HIBERNATE;

	// todo : what would we like to make configurable at this level?

	private final Project project;
	private final DirectoryProperty outputDirectory;
	private final EnhancementSpec enhancementDsl;
	private final JpaStaticMetamodelGenerationSpec metamodelGenDsl;

	@Inject
	public HibernateOrmSpec(Project project) {
		outputDirectory = project.getObjects().directoryProperty();
		this.project = project;
		// default...
		outputDirectory.convention(
				project.provider(
						() -> {
							final DirectoryProperty buildDirProp = project.getLayout().getBuildDirectory();
							return buildDirProp.dir( DEFAULT_OUTPUT_PATH ).get();
						}
				)
		);

		enhancementDsl = getExtensions().create( EnhancementSpec.DSL_NAME, EnhancementSpec.class, this, project );
		metamodelGenDsl = getExtensions().create( JpaStaticMetamodelGenerationSpec.DSL_NAME, JpaStaticMetamodelGenerationSpec.class, this, project );
	}

	public EnhancementSpec getEnhancementDsl() {
		return enhancementDsl;
	}

	public void enhancement(Action<EnhancementSpec> action) {
		action.execute( enhancementDsl );
	}

	public JpaStaticMetamodelGenerationSpec getMetamodelGenDsl() {
		return metamodelGenDsl;
	}

	public void metamodelGen(Action<JpaStaticMetamodelGenerationSpec>action) {
		action.execute( metamodelGenDsl );
	}

	public DirectoryProperty getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(Directory directory) {
		outputDirectory.set( directory );
	}

	public void outputDirectory(Directory directory) {
		setOutputDirectory( directory );
	}

	public void setOutputDirectory(String path) {
		setOutputDirectory( project.getLayout().getBuildDirectory().dir( path ).get() );
	}

	public void outputDirectory(String path) {
		setOutputDirectory( path );
	}

	@Override
	public abstract ExtensionContainer getExtensions();
}
