/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idprops;

import javax.persistence.criteria.CriteriaQuery;

import org.junit.Test;

import org.hibernate.query.Query;
import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class IdentifierPropertyReferencesTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "idprops/Mapping.hbm.xml" };
	}

	@Test
	public void testHqlIdPropertyReferences() {
		Person p = new Person( new Long( 1 ), "steve", 123 );
		Order o = new Order( new Long( 1 ), p );
		inTransaction(
				s -> {
					s.save( p );
					LineItem l = new LineItem( o, "my-product", 2 );
					l.setId( "456" );
					s.save( o );
				}
		);

		inTransaction(
				s -> {
					long count = extractCount( s, "select count(*) from Person p where p.id = 123" );
					assertEquals( "Person by id prop (non-identifier)", 1, count );
					count = extractCount( s, "select count(*) from Person p where p.pk = 1" );
					assertEquals( "Person by pk prop (identifier)", 1, count );

					count = extractCount( s, "select count(*) from Order o where o.id = 1" );
					assertEquals( "Order by number prop (named identifier)", 1, count );
					count = extractCount( s, "select count(*) from Order o where o.number = 1" );
					assertEquals( "Order by id prop (virtual identifier)", 1, count );

					count = extractCount( s, "select count(*) from LineItem l where l.id = '456'" );
					assertEquals( "LineItem by id prop (non-identifier", 1, count );

					if ( getDialect().supportsRowValueConstructorSyntax() ) {
						Query q = s.createQuery( "select count(*) from LineItem l where l.pk = (:order, :product)" )
								.setParameter( "order", o )
								.setParameter( "product", "my-product" );
						count = extractCount( q );
						assertEquals( "LineItem by pk prop (named composite identifier", 1, count );
					}

					count = extractCount( s, "select count(*) from Order o where o.orderee.id = 1" );
					assertEquals( 0, count );
					count = extractCount( s, "select count(*) from Order o where o.orderee.pk = 1" );
					assertEquals( 1, count );
					count = extractCount( s, "select count(*) from Order o where o.orderee.id = 123" );
					assertEquals( 1, count );

					count = extractCount( s, "select count(*) from LineItem l where l.pk.order.id = 1" );
					assertEquals( 1, count );
					count = extractCount( s, "select count(*) from LineItem l where l.pk.order.number = 1" );
					assertEquals( 1, count );
					count = extractCount( s, "select count(*) from LineItem l where l.pk.order.orderee.pk = 1" );
					assertEquals( 1, count );

					s.delete( o );
					s.delete( p );
				}
		);
	}

	private long extractCount(Session s, String hql) {
		return extractCount( s.createQuery( hql ) );
	}

	private long extractCount(Query query) {
		return ( (Long) query.list().get( 0 ) ).longValue();
	}

	private long extractCount(Session s, CriteriaQuery crit) {
		Query query = s.createQuery( crit );
		return ( (Long) query.list().get( 0 ) ).longValue();
	}
}
