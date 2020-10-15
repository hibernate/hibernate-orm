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
import org.gradle.api.provider.Property;

import org.hibernate.orm.tooling.gradle.enhance.EnhancementSpec;
import org.hibernate.orm.tooling.gradle.metamodel.JpaMetamodelGenerationSpec;

/**
 * Main DSL extension for Hibernate ORM.  Available as `project.hibernate`
 */
public abstract class HibernateOrmSpec implements ExtensionAware {
	public static final String HIBERNATE = "hibernate";

	public static final String DSL_NAME = HIBERNATE;

	private final Property<String> hibernateVersionProperty;
	private final Property<Boolean> supportEnhancementProperty;
	private final Property<Boolean> supportJpaMetamodelProperty;

	private final EnhancementSpec enhancementDsl;
	private final JpaMetamodelGenerationSpec jpaMetamodelDsl;


	@Inject
	@SuppressWarnings( "UnstableApiUsage" )
	public HibernateOrmSpec(Project project) {
		hibernateVersionProperty = project.getObjects().property( String.class );
		hibernateVersionProperty.convention( HibernateVersion.version );

		supportEnhancementProperty = project.getObjects().property( Boolean.class );
		supportEnhancementProperty.convention( true );

		supportJpaMetamodelProperty = project.getObjects().property( Boolean.class );
		supportJpaMetamodelProperty.convention( true );

		enhancementDsl = getExtensions().create( EnhancementSpec.DSL_NAME, EnhancementSpec.class, this, project );
		jpaMetamodelDsl = getExtensions().create( JpaMetamodelGenerationSpec.DSL_NAME, JpaMetamodelGenerationSpec.class, this, project );
	}

	public Property<String> getHibernateVersionProperty() {
		return hibernateVersionProperty;
	}

	public void hibernateVersion(String version) {
		setHibernateVersion( version );
	}

	public void setHibernateVersion(String version) {
		hibernateVersionProperty.set( version );
	}

	public Property<Boolean> getSupportEnhancementProperty() {
		return supportEnhancementProperty;
	}

	public void disableEnhancement() {
		supportEnhancementProperty.set( false );
	}

	public Property<Boolean> getSupportJpaMetamodelProperty() {
		return supportJpaMetamodelProperty;
	}

	public void disableJpaMetamodel() {
		supportJpaMetamodelProperty.set( false );
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
