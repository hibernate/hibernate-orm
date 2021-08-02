/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.antlr;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;

/**
 * @author Steve Ebersole
 */
public class Antlr4Spec {
	public static final String REGISTRATION_NAME = "antlr4";

	private final DirectoryProperty grammarBaseDirectory;
	private final DirectoryProperty outputBaseDirectory;

	private final NamedDomainObjectContainer<GrammarDescriptor> grammarDescriptors;

	@SuppressWarnings("UnstableApiUsage")
	public Antlr4Spec(ObjectFactory objectFactory, ProjectLayout layout) {
		grammarBaseDirectory = objectFactory.directoryProperty();
		grammarBaseDirectory.convention( layout.getProjectDirectory().dir( "src/main/antlr" ) );

		outputBaseDirectory = objectFactory.directoryProperty();
		outputBaseDirectory.convention( layout.getBuildDirectory().dir( "generated/sources/antlr/main" ) );

		grammarDescriptors = objectFactory.domainObjectContainer( GrammarDescriptor.class );
	}

	public DirectoryProperty getGrammarBaseDirectory() {
		return grammarBaseDirectory;
	}

	public DirectoryProperty getOutputBaseDirectory() {
		return outputBaseDirectory;
	}

	public NamedDomainObjectContainer<GrammarDescriptor> getGrammarDescriptors() {
		return grammarDescriptors;
	}
}
