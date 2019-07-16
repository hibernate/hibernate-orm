/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idprops;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.Test;

import org.hibernate.query.Query;
import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class CriteriaIdPropertyReferencesTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "idprops/Mapping.hbm.xml" };
	}


	@Test
	public void testCriteriaIdPropertyReferences() {
		Session s = openSession();
		s.beginTransaction();
		Person p = new Person( new Long(1), "steve", 123 );
		s.save( p );
		Order o = new Order( new Long(1), p );
		LineItem l = new LineItem( o, "my-product", 2 );
		l.setId( "456" );
		s.save( o );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

		Criteria crit = s.createCriteria( Person.class );
		crit.setProjection( Projections.rowCount() );
		crit.add( Restrictions.eq( "id", new Integer(123) ) );
		long count = extractCount( crit );
		assertEquals( "Person by id prop (non-identifier)", 1, count );

		crit = s.createCriteria( Person.class );
		crit.setProjection( Projections.rowCount() );
		crit.add( Restrictions.eq( "pk", new Long(1) ) );
		count = extractCount( crit );
		assertEquals( "Person by pk prop (identifier)", 1, count );

		crit = s.createCriteria( Order.class );
		crit.setProjection( Projections.rowCount() );
		crit.add(  Restrictions.eq( "number", new Long(1) ) );
		count = extractCount( crit );
		assertEquals( "Order by number prop (named identifier)", 1, count );

		crit = s.createCriteria( Order.class );
		crit.setProjection( Projections.rowCount() );
		crit.add(  Restrictions.eq( "id", new Long(1) ) );
		count = extractCount( crit );
		assertEquals( "Order by id prop (virtual identifier)", 1, count );

		crit = s.createCriteria( LineItem.class );
		crit.setProjection( Projections.rowCount() );
		crit.add(  Restrictions.eq( "id", "456" ) );
		count = extractCount( crit );
		assertEquals( "LineItem by id prop (non-identifier", 1, count );

		if ( getDialect().supportsRowValueConstructorSyntax() ) {
			crit = s.createCriteria( LineItem.class );
			crit.setProjection( Projections.rowCount() );
			crit.add( Restrictions.eq( "pk", new LineItemPK( o, "my-product" ) ) );
			count = extractCount( crit );
			assertEquals( "LineItem by pk prop (named composite identifier)", 1, count );
		}

		crit = s.createCriteria( Order.class );
		crit.setProjection( Projections.rowCount() );
		crit.createAlias( "orderee", "p" ).add( Restrictions.eq( "p.id", new Integer(1) ) );
		count = extractCount( crit );
		assertEquals( 0, count );

		crit = s.createCriteria( Order.class );
		crit.setProjection( Projections.rowCount() );
		crit.createAlias( "orderee", "p" ).add( Restrictions.eq( "p.pk", new Long(1) ) );
		count = extractCount( crit );
		assertEquals( 1, count );

		crit = s.createCriteria( Order.class );
		crit.setProjection( Projections.rowCount() );
		crit.createAlias( "orderee", "p" ).add( Restrictions.eq( "p.id", new Integer(123) ) );
		count = extractCount( crit );
		assertEquals( 1, count );

		crit = s.createCriteria( LineItem.class );
		crit.setProjection( Projections.rowCount() );
		crit.add( Restrictions.eq( "pk.order.id", new Long(1) ) );
		count = extractCount( crit );
		assertEquals( 1, count );

		crit = s.createCriteria( LineItem.class );
		crit.setProjection( Projections.rowCount() );
		crit.add( Restrictions.eq( "pk.order.number", new Long(1) ) );
		count = extractCount( crit );
		assertEquals( 1, count );

		s.delete( o );
		s.delete( p );
		s.getTransaction().commit();
		s.close();
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
