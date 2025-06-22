/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand.identity;

import org.junit.Test;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.orm.test.sql.hand.Organization;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.fail;

/**
 * Custom SQL tests for combined usage of custom insert SQL and identity columns
 *
 * @author Gail Badner
 */
@RequiresDialectFeature( DialectChecks.SupportsIdentityColumns.class )
public class CustomInsertSQLWithIdentityColumnTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public String[] getMappings() {
		return new String[] {"sql/hand/identity/Mappings.hbm.xml"};
	}

	@Test
	public void testBadInsertionFails() {
		Session session = openSession();
		session.beginTransaction();
		Organization org = new Organization( "hola!" );
		try {
			session.persist( org );
			session.remove( org );
			fail( "expecting bad custom insert statement to fail" );
		}
		catch( JDBCException e ) {
			// expected failure
		}

		session.getTransaction().rollback();
		session.close();
	}
}
