/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode;

import java.text.ParseException;

import org.hibernate.Hibernate;
import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class InvocationTargetExceptionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public String[] getMappings() {
		return new String[] { "bytecode/Bean.hbm.xml" };
	}

	@Test
	public void testProxiedInvocationException() {
		Session s = openSession();
		s.beginTransaction();
		Bean bean = new Bean();
		bean.setSomeString( "my-bean" );
		s.persist( bean );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		bean = ( Bean ) s.getReference( Bean.class, bean.getSomeString() );
		assertFalse( Hibernate.isInitialized( bean ) );
		try {
			bean.throwException();
			fail( "exception not thrown" );
		}
		catch ( ParseException e ) {
			// expected behavior
		}
		catch ( Throwable t ) {
			fail( "unexpected exception type : " + t );
		}

		s.remove( bean );
		s.getTransaction().commit();
		s.close();
	}
}
