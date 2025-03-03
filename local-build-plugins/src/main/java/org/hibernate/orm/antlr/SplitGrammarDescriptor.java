/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.antlr;

import javax.inject.Inject;

import org.gradle.api.Named;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

/**
 * @author Steve Ebersole
 */
public class SplitGrammarDescriptor implements Named {
	private final String name;
	private final AntlrSpec antlrSpec;

	private final Property<String> packageName;
	private final Property<String> lexerFileName;
	private final Property<String> parserFileName;

	private final Property<Boolean> generateVisitor;
	private final Property<Boolean> generateListener;

	@Inject
	public SplitGrammarDescriptor(String name, AntlrSpec antlrSpec, ObjectFactory objectFactory) {
		this.name = name;
		this.antlrSpec = antlrSpec;

		packageName = objectFactory.property( String.class );
		lexerFileName = objectFactory.property( String.class );
		parserFileName = objectFactory.property( String.class );

		generateVisitor = objectFactory.property( Boolean.class );
		generateVisitor.convention( true );

		generateListener = objectFactory.property( Boolean.class );
		generateListener.convention( true );
	}

	@Override
	public String getName() {
		return name;
	}

	public Property<String> getPackageName() {
		return packageName;
	}

	public Property<String> getLexerFileName() {
		return lexerFileName;
	}

	public Property<String> getParserFileName() {
		return parserFileName;
	}

	public Property<Boolean> getGenerateVisitor() {
		return generateVisitor;
	}

	public Property<Boolean> getGenerateListener() {
		return generateListener;
	}

	public Property<Boolean> generateVisitor() {
		return generateListener;
	}

	public Property<Boolean> generateListener() {
		return generateListener;
	}
}
