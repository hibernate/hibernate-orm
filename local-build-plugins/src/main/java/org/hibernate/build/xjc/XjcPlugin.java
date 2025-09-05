/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build.xjc;

import org.gradle.api.Plugin;
import org.gradle.api.Project;


/**
 * Plugin for executing the XJC process as part of the build.
 *
 * @author Steve Ebersole
 */
public class XjcPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		// Create the Plugin extension object (for users to configure our execution).
		project.getExtensions().create( "xjc", XjcExtension.class, project );
	}
}
