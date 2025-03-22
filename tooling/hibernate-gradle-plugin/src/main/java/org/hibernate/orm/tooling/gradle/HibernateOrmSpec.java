/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle;

import java.util.Arrays;

import org.gradle.api.Action;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.SourceSet;

import org.hibernate.orm.tooling.gradle.enhance.EnhancementSpec;

/**
 * Main DSL extension for Hibernate ORM.  Available as `project.hibernate`
 */
public abstract class HibernateOrmSpec {
	public static final String DSL_NAME = "hibernate";

	private boolean enhancementConfigured = false;

	public HibernateOrmSpec() {
		getUseSameVersion().convention( true );
		getSourceSet().convention( SourceSet.MAIN_SOURCE_SET_NAME );
		getLanguages().convention( Arrays.asList( "java", "kotlin" ) );
	}

	/**
	 * DSL extension for configuring bytecode enhancement.  Also acts as the trigger for
	 * opting into bytecode enhancement
	 */
	@Nested
	abstract public EnhancementSpec getEnhancement();

	/**
	 * Should the plugin inject a dependency on the same version of `hibernate-core`
	 * as the version of this plugin?  The dependency is added to the `implementation`
	 * {@link org.gradle.api.artifacts.Configuration}.
	 * <p>
	 * Defaults to {@code true}.  If overriding and performing enhancement, be aware
	 * that Hibernate generally only supports using classes enhanced using matching
	 * versions between tooling and runtime.  In other words, be careful.
	 */
	abstract public Property<Boolean> getUseSameVersion();

	/**
	 * The source-set name containing the domain model.  Defaults to the `main` source-set
	 */
	abstract public Property<String> getSourceSet();

	/**
	 * The languages used in the project
	 */
	abstract public SetProperty<String> getLanguages();

	/**
	 * @see #getEnhancement()
	 */
	@SuppressWarnings("unused")
	public void enhancement(Action<EnhancementSpec> action) {
		enhancementConfigured = true;
		action.execute( getEnhancement() );
	}


	public boolean isEnhancementEnabled() {
		return enhancementConfigured;
	}
}
