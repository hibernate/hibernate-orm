/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idprops;
import org.hibernate.Session;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/idprops/Mapping.hbm.xml"
)
@SessionFactory
public class IdentifierPropertyReferencesTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testHqlIdPropertyReferences(SessionFactoryScope scope) {
		Person p = new Person( new Long( 1 ), "steve", 123 );
		Order o = new Order( new Long( 1 ), p );
		scope.inTransaction(
				s -> {
					s.persist( p );
					LineItem l = new LineItem( o, "my-product", 2 );
					l.setId( "456" );
					s.persist( o );
				}
		);

		scope.inTransaction(
				s -> {
					long count = extractCount( s, "select count(*) from Person p where p.id = 123" );
					assertEquals( 1, count, "Person by id prop (non-identifier)" );
					count = extractCount( s, "select count(*) from Person p where p.pk = 1" );
					assertEquals( 1, count, "Person by pk prop (identifier)" );

					count = extractCount( s, "select count(*) from Order o where o.id = 1" );
					assertEquals( 1, count, "Order by number prop (named identifier)" );
					count = extractCount( s, "select count(*) from Order o where o.number = 1" );
					assertEquals( 1, count, "Order by id prop (virtual identifier)" );

					count = extractCount( s, "select count(*) from LineItem l where l.id = '456'" );
					assertEquals( 1, count, "LineItem by id prop (non-identifier" );

					Query q = s.createQuery( "select count(*) from LineItem l where l.pk = (:order, :product)" )
							.setParameter( "order", o )
							.setParameter( "product", "my-product" );
					count = extractCount( q );
					assertEquals( 1, count, "LineItem by pk prop (named composite identifier" );

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

					s.remove( o );
					s.remove( p );
				}
		);
	}

	private long extractCount(Session s, String hql) {
		return extractCount( s.createQuery( hql ) );
	}

	private long extractCount(Query query) {
		return ( (Long) query.list().get( 0 ) ).longValue();
	}

}
