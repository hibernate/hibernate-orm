/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build.maven.embedder;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.util.ArrayList;
import java.util.List;

public abstract class RunMavenTask extends DefaultTask {

	@ServiceReference
	abstract Property<MavenEmbedderService> getMavenEmbedderService();

	@Input
	abstract Property<String> getGoals();

	@Input
	abstract ListProperty<String> getArguments();

	@TaskAction
	public void run() {
		getMavenEmbedderService().get().execute( constructTaskAndArgs() );
	}

	private String[] constructTaskAndArgs() {
		List<String> args = new ArrayList<String>();
		args.add( getGoals().get() );
		args.addAll( getArguments().get() );
		return args.toArray(new String[0]);
	}

}
