/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle;

import java.util.Arrays;
import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.SourceSet;

import org.hibernate.orm.tooling.gradle.enhance.EnhancementSpec;

/**
 * Main DSL extension for Hibernate ORM.  Available as `project.hibernate`
 */
public abstract class HibernateOrmSpec implements ExtensionAware {
	public static final String HIBERNATE = "hibernate";

	public static final String DSL_NAME = HIBERNATE;

	private final Project project;

	private EnhancementSpec enhancementDsl;

	private final Property<Boolean> useSameVersion;
	private final Property<SourceSet> sourceSet;
	private final SetProperty<String> languages;

	private final Provider<EnhancementSpec> enhancementDslAccess;


	@Inject
	public HibernateOrmSpec(Project project) {
		this.project = project;

		useSameVersion = project.getObjects().property( Boolean.class );
		useSameVersion.convention( true );

		sourceSet = project.getObjects().property( SourceSet.class );
		sourceSet.convention( mainSourceSet( project ) );

		languages = project.getObjects().setProperty( String.class );
		languages.convention( Arrays.asList( "java", "kotlin" ) );

		enhancementDslAccess = project.provider( () -> enhancementDsl );
	}

	private static SourceSet mainSourceSet(Project project) {
		return resolveSourceSet( SourceSet.MAIN_SOURCE_SET_NAME, project );
	}

	private static SourceSet resolveSourceSet(String name, Project project) {
		final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType( JavaPluginExtension.class );
		return javaPluginExtension.getSourceSets().getByName( name );
	}

	@Override
	public abstract ExtensionContainer getExtensions();



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
	 * The source-set containing the domain model.  Defaults to the `main` source-set
	 */
	public Property<SourceSet> getSourceSet() {
		return sourceSet;
	}

	/**
	 * The languages used in the project
	 */
	public SetProperty<String> getLanguages() {
		return languages;
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
	 * @see #getEnhancement()
	 */
	public void enhancement(Action<EnhancementSpec> action) {
		action.execute( getEnhancement() );
	}


	public boolean isEnhancementEnabled() {
		return enhancementDsl != null;
	}


	/**
	 * @see #getUseSameVersion()
	 *
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void setUseSameVersion(boolean value) {
		useSameVersion.set( value );
	}

	/**
	 * @see #getUseSameVersion()
	 *
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void useSameVersion() {
		useSameVersion.set( true );
	}

	/**
	 * @see #getSourceSet()
	 *
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void setSourceSet(String name) {
		setSourceSet( resolveSourceSet( name, project ) );
	}

	/**
	 * @see #getSourceSet()
	 *
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void setSourceSet(SourceSet sourceSet) {
		this.sourceSet.set( sourceSet );
	}

	/**
	 * @see #getSourceSet()
	 *
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void sourceSet(String name) {
		setSourceSet( resolveSourceSet( name, project ) );
	}

	/**
	 * @see #getSourceSet()
	 *
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void sourceSet(SourceSet sourceSet) {
		setSourceSet( sourceSet );
	}

	/**
	 * @see #getLanguages()
	 *
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated(forRemoval = true)
	public void setLanguages(Iterable<String> languages) {
		this.languages.set( languages );
	}

	/**
	 * @see #getLanguages()
	 *
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated
	public void languages(String language) {
		this.languages.add( language );
	}

	/**
	 * Provider access to {@link #getEnhancement()}
	 *
	 * @deprecated See the Gradle property naming <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_configuration_faqs">guidelines</a>
	 */
	@Deprecated
	public Provider<EnhancementSpec> getEnhancementDslAccess() {
		return enhancementDslAccess;
	}
}
