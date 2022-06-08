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
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;

import org.hibernate.orm.tooling.gradle.enhance.EnhancementSpec;
import org.hibernate.orm.tooling.gradle.metamodel.JpaMetamodelGenerationSpec;

/**
 * Main DSL extension for Hibernate ORM.  Available as `project.hibernate`
 */
public abstract class HibernateOrmSpec implements ExtensionAware {
	public static final String HIBERNATE = "hibernate";

	public static final String DSL_NAME = HIBERNATE;

	private final Project project;

	private EnhancementSpec enhancementDsl;
	private JpaMetamodelGenerationSpec jpaMetamodelDsl;

	private final Property<Boolean> useSameVersion;
	private final Property<SourceSet> sourceSet;

	private final Provider<EnhancementSpec> enhancementDslAccess;
	private final Provider<JpaMetamodelGenerationSpec> jpaMetamodelDslAccess;


	@Inject
	public HibernateOrmSpec(Project project) {
		this.project = project;

		useSameVersion = project.getObjects().property( Boolean.class );
		useSameVersion.convention( true );

		sourceSet = project.getObjects().property( SourceSet.class );
		sourceSet.convention( mainSourceSet( project ) );

		enhancementDslAccess = project.provider( () -> enhancementDsl );
		jpaMetamodelDslAccess = project.provider( () -> jpaMetamodelDsl );
	}

	private static SourceSet mainSourceSet(Project project) {
		return resolveSourceSet( SourceSet.MAIN_SOURCE_SET_NAME, project );
	}

	private static SourceSet resolveSourceSet(String name, Project project) {
		final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType( JavaPluginExtension.class );
		return javaPluginExtension.getSourceSets().getByName( name );
	}

	/**
	 * Should the plugin inject a dependency on the same version of `hibernate-core`
	 * as the version of this plugin?  The dependency is added to the `implementation`
	 * {@link org.gradle.api.artifacts.Configuration}.
	 * <p>
	 * Defaults to {@code true}.  If overriding and performing enhancement, be aware
	 * that Hibernate generally only supports using classes enhanced using matching
	 * versions between tooling and runtime.  In other words, be careful.
	 */
	public Property<Boolean> getUseSameVersion() {
		return useSameVersion;
	}

	/**
	 * @see #getUseSameVersion()
	 */
	public void setUseSameVersion(boolean value) {
		useSameVersion.set( value );
	}

	/**
	 * @see #getUseSameVersion()
	 */
	public void useSameVersion() {
		useSameVersion.set( true );
	}

	/**
	 * The source-set containing the domain model.  Defaults to the `main` source-set
	 */
	public Property<SourceSet> getSourceSet() {
		return sourceSet;
	}

	/**
	 * @see #getSourceSet()
	 */
	public void setSourceSet(String name) {
		setSourceSet( resolveSourceSet( name, project ) );
	}

	/**
	 * @see #getSourceSet()
	 */
	public void setSourceSet(SourceSet sourceSet) {
		this.sourceSet.set( sourceSet );
	}

	/**
	 * @see #getSourceSet()
	 */
	public void sourceSet(String name) {
		setSourceSet( resolveSourceSet( name, project ) );
	}

	/**
	 * @see #getSourceSet()
	 */
	public void sourceSet(SourceSet sourceSet) {
		setSourceSet( sourceSet );
	}

	/**
	 * DSL extension for configuring bytecode enhancement.  Also acts as the trigger for
	 * opting into bytecode enhancement
	 */
	public EnhancementSpec getEnhancement() {
		if ( enhancementDsl == null ) {
			enhancementDsl = getExtensions().create( EnhancementSpec.DSL_NAME, EnhancementSpec.class, this, project );
		}

		return enhancementDsl;
	}

	/**
	 * Provider access to {@link #getEnhancement()}
	 */
	public Provider<EnhancementSpec> getEnhancementDslAccess() {
		return enhancementDslAccess;
	}

	public boolean isEnhancementEnabled() {
		return enhancementDsl != null;
	}

	/**
	 * @see #getEnhancement()
	 */
	public void enhancement(Action<EnhancementSpec> action) {
		action.execute( getEnhancement() );
	}

	/**
	 * DSL extension for configuring JPA static metamodel generation.  Also acts as the trigger for
	 * opting into the generation
	 */
	public JpaMetamodelGenerationSpec getJpaMetamodel() {
		if ( jpaMetamodelDsl == null ) {
			jpaMetamodelDsl = getExtensions().create( JpaMetamodelGenerationSpec.DSL_NAME, JpaMetamodelGenerationSpec.class, this, project );
		}
		return jpaMetamodelDsl;
	}


	/**
	 * Provider access to {@link #getJpaMetamodel()}
	 */
	public Provider<JpaMetamodelGenerationSpec> getJpaMetamodelDslAccess() {
		return jpaMetamodelDslAccess;
	}

	public boolean isMetamodelGenerationEnabled() {
		return jpaMetamodelDsl != null;
	}

	/**
	 * @see #getJpaMetamodel()
	 */
	public void jpaMetamodel(Action<JpaMetamodelGenerationSpec>action) {
		action.execute( getJpaMetamodel() );
	}

	@Override
	public abstract ExtensionContainer getExtensions();
}
