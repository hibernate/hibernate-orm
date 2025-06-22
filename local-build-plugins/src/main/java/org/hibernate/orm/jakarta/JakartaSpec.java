/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.jakarta;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;

/**
 * Gradle DSL extension for configuring the Jakartafying transformations
 * needed for a project.  Mainly used to trigger different conventions depending
 * on whether the "source" is a local project or an external module
 *
 * NOTE : abstract to allow Gradle to "weave in" behavior (like being an extension container, etc)
 *
 * @author Steve Ebersole
 */
public abstract class JakartaSpec {
	public static final String REGISTRATION_NAME = "jakarta";

	private final Project jakartaProject;

	private Dependency sourceDependency;

	@Inject
	public JakartaSpec(Project jakartaProject) {
		// `jakartaProject` is the project where this plugin gets applied - the `-jakarta` one
		this.jakartaProject = jakartaProject;
	}

	public void source(Object dependencyNotation) {
		if ( sourceDependency != null ) {
			throw new IllegalStateException( "Source Dependency already specified" );
		}

		sourceDependency = jakartaProject.getDependencies().create( dependencyNotation );
	}

	public Dependency getSourceDependency() {
		return sourceDependency;
	}
}
