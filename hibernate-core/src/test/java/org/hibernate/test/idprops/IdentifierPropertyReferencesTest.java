/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idprops;

import org.hibernate.Session;
import org.hibernate.orm.test.idprops.LineItem;
import org.hibernate.orm.test.idprops.Order;
import org.hibernate.orm.test.idprops.Person;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
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
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Person" ).executeUpdate();
					session.createQuery( "delete from LineItem" ).executeUpdate();
					session.createQuery( "delete from Order" ).executeUpdate();
				}
		);
	}

	@Test
	public void testHqlIdPropertyReferences(SessionFactoryScope scope) {
		Person p = new Person( new Long( 1 ), "steve", 123 );
		Order o = new Order( new Long( 1 ), p );
		scope.inTransaction(
				s -> {
					s.save( p );
					LineItem l = new LineItem( o, "my-product", 2 );
					l.setId( "456" );
					s.save( o );
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

					if ( new DialectFeatureChecks.SupportsRowValueConstructorSyntaxCheck().apply( scope.getSessionFactory()
																										  .getJdbcServices()
																										  .getDialect() ) ) {
						Query q = s.createQuery( "select count(*) from LineItem l where l.pk = (:order, :product)" )
								.setParameter( "order", o )
								.setParameter( "product", "my-product" );
						count = extractCount( q );
						assertEquals( 1, count, "LineItem by pk prop (named composite identifier" );
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

}
