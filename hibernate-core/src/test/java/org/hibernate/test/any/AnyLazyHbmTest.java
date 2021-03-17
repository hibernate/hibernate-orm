/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.any;

import org.hibernate.LazyInitializationException;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.annotations.any.IntegerProperty;
import org.hibernate.test.annotations.any.LazyPropertySet;
import org.hibernate.test.annotations.any.Property;
import org.hibernate.test.annotations.any.StringProperty;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class AnyLazyHbmTest extends BaseCoreFunctionalTestCase {

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	public void testFetchLazy() {
		doInHibernate( this::sessionFactory, s -> {
			LazyPropertySet set = new LazyPropertySet( "string" );
			Property property = new StringProperty( "name", "Alex" );
			set.setSomeProperty( property );
			s.save( set );
		} );

		LazyPropertySet result = doInHibernate( this::sessionFactory, s -> {
			return s.createQuery( "select s from LazyPropertySet s where name = :name", LazyPropertySet.class )
					.setParameter( "name", "string" )
					.getSingleResult();
		} );

		assertNotNull( result );
		assertNotNull( result.getSomeProperty() );

		try {
			result.getSomeProperty().asString();
			fail( "should not get the property string after session closed." );
		}
		catch (LazyInitializationException e) {
			// expected
		}
		catch (Exception e) {
			fail( "should not throw exception other than LazyInitializationException." );
		}
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
				"any/AnyTestLazyPropertySet.hbm.xml"
		};
	}
}
