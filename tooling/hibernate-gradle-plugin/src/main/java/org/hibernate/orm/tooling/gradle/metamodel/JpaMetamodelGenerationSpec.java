/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle.metamodel;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;

import org.hibernate.orm.tooling.gradle.HibernateOrmSpec;

/**
 * @author Steve Ebersole
 */
public class JpaMetamodelGenerationSpec {
	public static final String DSL_NAME = "jpaMetamodel";

	private final DirectoryProperty outputDirectory;

	@Inject
	public JpaMetamodelGenerationSpec(HibernateOrmSpec ormDsl, Project project) {
		this.outputDirectory = project.getObjects().directoryProperty();
		outputDirectory.convention(
				project.getLayout().getBuildDirectory().dir( "generated/sources/jpaMetamodel" )
		);
	}

	public DirectoryProperty getOutputDirectory() {
		return outputDirectory;
	}
}
