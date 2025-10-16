/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.antlr;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.TaskProvider;

/**
 * @author Steve Ebersole
 */
public class AntlrSpec {
	public static final String REGISTRATION_NAME = "antlr4";

	private final DirectoryProperty grammarBaseDirectory;
	private final DirectoryProperty outputBaseDirectory;

	private final NamedDomainObjectContainer<SplitGrammarDescriptor> grammarDescriptors;

	public AntlrSpec(ProjectLayout layout, ObjectFactory objectFactory, TaskProvider<Task> groupingTask, Project project) {
		grammarBaseDirectory = objectFactory.directoryProperty();
		grammarBaseDirectory.convention( layout.getProjectDirectory().dir( "src/main/antlr" ) );

		outputBaseDirectory = objectFactory.directoryProperty();
		outputBaseDirectory.convention( layout.getBuildDirectory().dir( "generated/sources/antlr/main" ) );

		grammarDescriptors = objectFactory.domainObjectContainer(
				SplitGrammarDescriptor.class,
				new GrammarDescriptorFactory( this, groupingTask, project )
		);
	}

	public DirectoryProperty getGrammarBaseDirectory() {
		return grammarBaseDirectory;
	}

	public DirectoryProperty getOutputBaseDirectory() {
		return outputBaseDirectory;
	}

	public NamedDomainObjectContainer<SplitGrammarDescriptor> getGrammarDescriptors() {
		return grammarDescriptors;
	}
}
