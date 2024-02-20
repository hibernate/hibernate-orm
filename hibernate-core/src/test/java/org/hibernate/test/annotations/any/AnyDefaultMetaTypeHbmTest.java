/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.any;

import javax.persistence.TypedQuery;

import org.hibernate.query.Query;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AnyDefaultMetaTypeHbmTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testDefaultAnyAssociation() {
		doInHibernate( this::sessionFactory, s -> {
			HbmPropertySet set1 = new HbmPropertySet( "string" );
			Property property = new StringProperty( "name", "Alex" );
			set1.setSomeProperty( property );
			s.save( set1 );

			HbmPropertySet set2 = new HbmPropertySet( "integer" );
			property = new IntegerProperty( "age", 33 );
			set2.setSomeProperty( property );
			s.save( set2 );
		} );

		doInHibernate( this::sessionFactory, s -> {
			TypedQuery<HbmPropertySet> q = s
					.createQuery( "select s from HbmPropertySet s where name = :name", HbmPropertySet.class );
			q.setParameter( "name", "string" );
			HbmPropertySet result = q.getSingleResult();

			assertNotNull( result );
			assertNotNull( result.getSomeProperty() );
			assertTrue( result.getSomeProperty() instanceof StringProperty );
			assertEquals( "Alex", result.getSomeProperty().asString() );

			q.setParameter( "name", "integer" );
			result = q.getSingleResult();

			assertNotNull( result );
			assertNotNull( result.getSomeProperty() );
			assertTrue( result.getSomeProperty() instanceof IntegerProperty );
			assertEquals( "33", result.getSomeProperty().asString() );
		} );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				StringProperty.class,
				IntegerProperty.class,
				LongProperty.class,
		};
	}

	@Override
	public String[] getMappings() {
		return new String[] {
				"annotations/any/AnyDefaultMetaTypeHbmTest.hbm.xml"
		};
	}
}
