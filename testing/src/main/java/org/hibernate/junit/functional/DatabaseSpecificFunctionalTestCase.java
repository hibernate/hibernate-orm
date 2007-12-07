/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.junit.functional;

import org.hibernate.junit.SkipLog;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public abstract class DatabaseSpecificFunctionalTestCase extends FunctionalTestCase {
	public DatabaseSpecificFunctionalTestCase(String string) {
		super( string );
	}

	protected void runTest() throws Throwable {
		// Note: this protection comes into play when running
		// tests individually.  The suite as a whole is already
		// "protected" by the fact that these tests are actually
		// filtered out of the suite
		if ( appliesTo( getDialect() ) ) {
			super.runTest();
		}
		else {
			SkipLog.LOG.warn( "skipping database-specific test [" + fullTestName() + "] for dialect [" + getDialect().getClass().getName() + "]" );
		}
	}
}
