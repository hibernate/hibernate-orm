/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jmx;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Steve Ebersole
 */
public class TrivialTest extends BaseUnitTestCase {
	@Test
	public void testService() throws Exception {
		HibernateService hs = new HibernateService();
		hs.setJndiName( "SessionFactory" );
		hs.setMapResources( "org/hibernate/jmx/Entity.hbm.xml" );
		hs.setShowSqlEnabled( "true" );
		hs.start();
		hs.stop();
		hs.setProperty( "foo", "bar" );
		hs.start();
		hs.stop();
		try {
			hs.setMapResources( "non-existent" );
			hs.start();
		}
		catch( Throwable t ) {
			// expected behavior
		}
		finally {
			hs.stop();
		}
	}
}
