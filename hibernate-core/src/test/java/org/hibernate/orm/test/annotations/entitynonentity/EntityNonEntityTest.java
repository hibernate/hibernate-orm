/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entitynonentity;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.UnknownEntityTypeException;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class EntityNonEntityTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testMix() {
		GSM gsm = new GSM();
		gsm.brand = "Sony";
		gsm.frequency = 900;
		gsm.isNumeric = true;
		gsm.number = 2;
		gsm.species = "human";
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( gsm );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		gsm = s.get( GSM.class, gsm.id );
		assertEquals( "top mapped superclass", 2, gsm.number );
		assertNull( "non entity between mapped superclass and entity", gsm.species );
		assertTrue( "mapped superclass under entity", gsm.isNumeric );
		assertNull( "non entity under entity", gsm.brand );
		assertEquals( "leaf entity", 900, gsm.frequency );
		s.remove( gsm );
		tx.commit();
		s.close();
	}

	@Test
	@JiraKey( value = "HHH-9856" )
	public void testGetAndFindNonEntityThrowsIllegalArgumentException() {
		try {
			sessionFactory().getMappingMetamodel().findEntityDescriptor(Cellular.class);
			sessionFactory().getMappingMetamodel().getEntityDescriptor( Cellular.class );

		}
		catch (UnknownEntityTypeException ignore) {
			// expected
		}

		try {
			sessionFactory().getRuntimeMetamodels().getMappingMetamodel().getEntityDescriptor( Cellular.class.getName() );
		}
		catch (UnknownEntityTypeException ignore) {
			// expected
		}

		Session s = openSession();
		s.beginTransaction();
		try {
			s.get( Cellular.class, 1 );
			fail( "Expecting a failure" );
		}
		catch (UnknownEntityTypeException ignore) {
			// expected
		}
		finally {
			s.getTransaction().commit();
			s.close();
		}

		s = openSession();
		s.beginTransaction();
		try {
			s.get( Cellular.class.getName(), 1 );
			fail( "Expecting a failure" );
		}
		catch (UnknownEntityTypeException ignore) {
			// expected
		}
		finally {
			s.getTransaction().commit();
			s.close();
		}
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Phone.class,
				Voice.class,
				// Adding Cellular here is a test for HHH-9855
				Cellular.class,
				GSM.class
		};
	}
}
