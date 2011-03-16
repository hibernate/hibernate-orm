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

import javax.transaction.SystemException;

import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;

import org.junit.After;
import org.junit.Rule;
import org.junit.runner.RunWith;

import org.hibernate.testing.TestLogger;
import org.hibernate.testing.jta.TestingJtaBootstrap;

/**
 * The most test adapter.  Applies both the {@link CustomRunner} and {@link Processor} rule.
 *
 * @author Steve Ebersole
 */
@RunWith( CustomRunner.class )
public abstract class BaseUnitTestCase {
	@Rule
	public Processor processor = new Processor();

	@After
	public void releaseTransactions() {
		if ( JtaStatusHelper.isActive( TestingJtaBootstrap.INSTANCE.getTransactionManager() ) ) {
			TestLogger.LOG.warn( "Cleaning up unfinished transaction" );
			try {
				TestingJtaBootstrap.INSTANCE.getTransactionManager().rollback();
			}
			catch (SystemException ignored) {
			}
		}
	}
}
