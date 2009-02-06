//$Id $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
 *
 */
package org.hibernate.test.dialect.unit.lockhint;

import junit.framework.TestSuite;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseASE15Dialect;

/**
 * {@inheritDoc}
 *
 * @author Gail Badner
 */
public class SybaseASE15LockHintsTest extends AbstractLockHintTest {
	public static final Dialect DIALECT = new SybaseASE15Dialect();

	public SybaseASE15LockHintsTest(String string) {
		super( string );
	}

	protected String getLockHintUsed() {
		return "holdlock";
	}

	protected Dialect getDialectUnderTest() {
		return DIALECT;
	}

	public static TestSuite suite() {
		return new TestSuite( SybaseASE15LockHintsTest.class );
	}
}
