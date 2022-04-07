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
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;

import org.hibernate.orm.tooling.gradle.enhance.EnhancementSpec;
import org.hibernate.orm.tooling.gradle.metamodel.JpaMetamodelGenerationSpec;

/**
 * Main DSL extension for Hibernate ORM.  Available as `project.hibernate`
 */
public abstract class HibernateOrmSpec implements ExtensionAware {
	public static final String HIBERNATE = "hibernate";

	public static final String DSL_NAME = HIBERNATE;

	private final Property<String> hibernateVersionProperty;
	private final Project project;
	private final Property<SourceSet> sourceSetProperty;
	private final Property<Boolean> supportEnhancementProperty;
	private final Property<Boolean> supportJpaMetamodelProperty;

	private final EnhancementSpec enhancementDsl;
	private final JpaMetamodelGenerationSpec jpaMetamodelDsl;


	@Inject
	public HibernateOrmSpec(Project project) {
		this.project = project;

		hibernateVersionProperty = project.getObjects().property( String.class );
		hibernateVersionProperty.convention( HibernateVersion.version );

		sourceSetProperty = project.getObjects().property( SourceSet.class );
		sourceSetProperty.convention( mainSourceSet( project ) );

		supportEnhancementProperty = project.getObjects().property( Boolean.class );
		supportEnhancementProperty.convention( true );

		supportJpaMetamodelProperty = project.getObjects().property( Boolean.class );
		supportJpaMetamodelProperty.convention( true );

		enhancementDsl = getExtensions().create( EnhancementSpec.DSL_NAME, EnhancementSpec.class, this, project );
		jpaMetamodelDsl = getExtensions().create( JpaMetamodelGenerationSpec.DSL_NAME, JpaMetamodelGenerationSpec.class, this, project );
	}

	private static SourceSet mainSourceSet(Project project) {
		return resolveSourceSet( SourceSet.MAIN_SOURCE_SET_NAME, project );
	}

	private static SourceSet resolveSourceSet(String name, Project project) {
		final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType( JavaPluginExtension.class );
		return javaPluginExtension.getSourceSets().getByName( name );
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

	public Property<SourceSet> getSourceSetProperty() {
		return sourceSetProperty;
	}

	public void setSourceSet(String name) {
		setSourceSet( resolveSourceSet( name, project ) );
	}

	public void setSourceSet(SourceSet sourceSet) {
		sourceSetProperty.set( sourceSet );
	}

	public void sourceSet(String name) {
		setSourceSet( resolveSourceSet( name, project ) );
	}

	public void sourceSet(SourceSet sourceSet) {
		setSourceSet( sourceSet );
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
