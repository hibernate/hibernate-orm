/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.junit4;

import org.junit.runners.model.Statement;

/**
 * @author Steve Ebersole
 */
public class BeforeClassCallbackHandler extends Statement {
	private final CustomRunner runner;
	private final Statement wrappedStatement;

	public BeforeClassCallbackHandler(CustomRunner runner, Statement wrappedStatement) {
		this.runner = runner;
		this.wrappedStatement = wrappedStatement;
	}

	@Override
	public void evaluate() throws Throwable {
		runner.getTestClassMetadata().performBeforeClassCallbacks( runner.getTestInstance() );
		wrappedStatement.evaluate();
	}
}
