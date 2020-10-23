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
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtensionContainer;

import org.hibernate.orm.tooling.gradle.enhance.EnhancementSpec;
import org.hibernate.orm.tooling.gradle.metamodel.JpaMetamodelGenerationSpec;

/**
 * Main DSL extension for Hibernate ORM.  Available as `project.hibernateOrm`
 */
public abstract class HibernateOrmSpec implements ExtensionAware {
	public static final String HIBERNATE = "hibernate";
	public static final String ORM = "orm";

	public static final String DSL_NAME = HIBERNATE;

	// todo : what would we like to make configurable at this level?

	private final EnhancementSpec enhancementDsl;
	private final JpaMetamodelGenerationSpec jpaMetamodelDsl;

	@Inject
	public HibernateOrmSpec(Project project) {
		enhancementDsl = getExtensions().create( EnhancementSpec.DSL_NAME, EnhancementSpec.class, this, project );
		jpaMetamodelDsl = getExtensions().create( JpaMetamodelGenerationSpec.DSL_NAME, JpaMetamodelGenerationSpec.class, this, project );
	}

	public EnhancementSpec getEnhancementSpec() {
		return enhancementDsl;
	}

	public void enhancement(Action<EnhancementSpec> action) {
		action.execute( enhancementDsl );
	}

	public JpaMetamodelGenerationSpec getJpaMetamodelSpec() {
		return jpaMetamodelDsl;
	}

	public void jpaMetamodel(Action<JpaMetamodelGenerationSpec>action) {
		action.execute( jpaMetamodelDsl );
	}

	@Override
	public abstract ExtensionContainer getExtensions();
}
