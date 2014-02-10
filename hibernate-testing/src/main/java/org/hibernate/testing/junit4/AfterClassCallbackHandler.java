/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.testing.junit4;

import org.hibernate.internal.SessionFactoryRegistry;

import org.junit.runners.model.Statement;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class AfterClassCallbackHandler extends Statement {
	private static final Logger log = Logger.getLogger( AfterClassCallbackHandler.class );

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
		if ( SessionFactoryRegistry.INSTANCE.hasRegistrations() ) {
			log.warnf(
					"SessionFactory may be leaked during execution of test : %s",
					runner.getTestClassMetadata().getTestClass().getName()
			);
		}
	}
}
