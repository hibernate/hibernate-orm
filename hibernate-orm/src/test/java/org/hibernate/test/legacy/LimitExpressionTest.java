/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;

import java.util.Iterator;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
public class LimitExpressionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Person.class};
	}

	@Test
	public void testLimitZero() throws Exception {
		TransactionUtil.doInHibernate( this::sessionFactory, s -> {
			Iterator iter = s.createQuery( "from Person p" )
					.setMaxResults( 0 )
					.iterate();
			int count = 0;
			while ( iter.hasNext() ) {
				iter.next();
				count++;
			}
			assertEquals( 0, count );
			final List list = s.createQuery( "select p from Person p" )
					.setMaxResults( 0 )
					.setFirstResult( 2 )
					.list();
			assertTrue( list.isEmpty() );
		} );
	}

	@Before
	public void prepareTest() throws Exception {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			Person p = new Person();
			session.save( p );
		} );
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		@GeneratedValue
		private Long id;
	}
}
