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
package org.hibernate.test.annotations.onetoone.hhh4851;

import org.junit.Test;

import org.hibernate.PropertyValueException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
@TestForIssue( jiraKey = "HHH-4851" )
public class HHH4851Test extends BaseCoreFunctionalTestCase {
	@Test
	public void testHHH4851() throws Exception {
		Session session = openSession();
		Transaction trx = session.beginTransaction();
		Owner org = new Owner();
		org.setName( "root" );
		session.saveOrUpdate( org );

		ManagedDevice lTerminal = new ManagedDevice();
		lTerminal.setName( "test" );
		lTerminal.setOwner( org );
		session.saveOrUpdate( lTerminal );

		Device terminal = new Device();
		terminal.setTag( "test" );
		terminal.setOwner( org );
		try {
			session.saveOrUpdate( terminal );
		}
		catch ( PropertyValueException e ) {
			fail( "not-null checking should not be raised: " + e.getMessage() );
		}
		trx.commit();
		session.close();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.CHECK_NULLABILITY, "true" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Hardware.class,
				DeviceGroupConfig.class,
				Hardware.class,
				ManagedDevice.class,
				Device.class,
				Owner.class
		};
	}
}
