/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
