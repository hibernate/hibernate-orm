/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build.xjc;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.util.LinkedHashMap;

/**
 * @author Steve Ebersole
 */
public class XjcPlugin implements Plugin<Project> {
	public static final String XJC_BASIC_PLUGIN = "org.patrodyne.jvnet:hisrc-basicjaxb-plugins:2.2.1";
	public static final String XJC_BASIC_TOOLS = "org.patrodyne.jvnet:hisrc-basicjaxb-tools:2.2.1";
	public static final String XJC_BASIC_ANT = "org.patrodyne.jvnet:hisrc-basicjaxb-ant:2.2.1";

	public static final String ANT_TASK_NAME = "org.jvnet.basicjaxb.xjc.XJC2Task";

	@Override
	public void apply(Project project) {
		// Create the xjc grouping task
		final Task groupingTask = project.getTasks().create( "xjc", xjcTask -> {
			xjcTask.setGroup( "xjc" );
			xjcTask.setDescription( "Grouping task for executing one-or-more XJC compilations" );
		} );

		// Create the Plugin extension object (for users to configure our execution).
		project.getExtensions().create( "xjc", XjcExtension.class, groupingTask, project );

		final DependencyHandler dependencyHandler = project.getDependencies();
		final Configuration antTaskDependencies = project.getConfigurations().detachedConfiguration(
				dependencyHandler.create( XJC_BASIC_ANT ),
				dependencyHandler.create( XJC_BASIC_PLUGIN ),
				dependencyHandler.create( XJC_BASIC_TOOLS ),
				dependencyHandler.gradleApi()
		);

		final LinkedHashMap<String, String> map = new LinkedHashMap<>( 3 );
		map.put( "name", "xjc" );
		map.put( "classname", ANT_TASK_NAME );
		map.put( "classpath", antTaskDependencies.getAsPath() );
		project.getAnt().invokeMethod( "taskdef", new Object[] { map } );
		project.getAnt().setSaveStreams( false );
	}
}
