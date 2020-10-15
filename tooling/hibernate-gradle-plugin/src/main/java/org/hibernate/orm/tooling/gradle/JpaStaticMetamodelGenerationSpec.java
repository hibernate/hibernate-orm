/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle;

import javax.inject.Inject;

import org.gradle.api.Project;

/**
 * @author Steve Ebersole
 */
public class JpaStaticMetamodelGenerationSpec {
	public static final String DSL_NAME = "generateJpaStaticMetamodel";

	private final HibernateOrmSpec ormDsl;
	private final Project project;

	@Inject
	public JpaStaticMetamodelGenerationSpec(HibernateOrmSpec ormDsl, Project project) {
		this.ormDsl = ormDsl;
		this.project = project;
	}
}
