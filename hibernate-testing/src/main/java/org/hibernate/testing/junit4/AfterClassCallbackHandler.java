/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.junit4;

import org.junit.runners.model.Statement;

/**
 * @author Steve Ebersole
 */
public class AfterClassCallbackHandler extends Statement {
	private final CustomRunner runner;
	private final Statement wrappedStatement;

	public AfterClassCallbackHandler(CustomRunner runner, Statement wrappedStatement) {
		this.runner = runner;
		this.wrappedStatement = wrappedStatement;
	}

	@Override
	public void evaluate() throws Throwable {
		wrappedStatement.evaluate();
		runner.getTestClassMetadata().performAfterClassCallbacks( runner.getTestInstance() );
	}
}
