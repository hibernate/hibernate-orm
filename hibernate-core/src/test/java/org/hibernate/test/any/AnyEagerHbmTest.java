/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.any;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.annotations.any.*;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AnyEagerHbmTest extends BaseCoreFunctionalTestCase {

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	public void testFetchEager() {
		doInHibernate( this::sessionFactory, s -> {
			org.hibernate.test.annotations.any.PropertySet set = new org.hibernate.test.annotations.any.PropertySet( "string" );
			Property property = new StringProperty( "name", "Alex" );
			set.setSomeProperty( property );
			s.save( set );
		} );

		org.hibernate.test.annotations.any.PropertySet result = doInHibernate( this::sessionFactory, s -> {
			return s.createQuery( "select s from PropertySet s where name = :name", org.hibernate.test.annotations.any.PropertySet.class )
					.setParameter( "name", "string" )
					.getSingleResult();
		} );

		assertNotNull( result );
		assertNotNull( result.getSomeProperty() );
		assertTrue( result.getSomeProperty() instanceof StringProperty );
		assertEquals( "Alex", result.getSomeProperty().asString() );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				StringProperty.class,
				IntegerProperty.class,
		};
	}

	@Override
	public String[] getMappings() {
		return new String[] {
				"any/AnyTestEagerPropertySet.hbm.xml"
		};
	}
}
