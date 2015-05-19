/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.polymorphism;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 * @author Brett Meyer
 */
public class PolymorphismTest extends BaseCoreFunctionalTestCase {
	
	@Test
	public void testPolymorphism() throws Exception {
		Car car = new Car();
		car.setModel( "SUV" );
		SportCar car2 = new SportCar();
		car2.setModel( "350Z" );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( car );
		s.persist( car2 );
		s.flush();
		assertEquals( 2, s.createQuery( "select car from Car car").list().size() );
		assertEquals( 0, s.createQuery( "select count(m) from " + Automobile.class.getName() + " m").list().size() );
		tx.rollback();
		s.close();

	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-7915")
	public void testNonPkInheritedFk() throws Exception {
		MarketRegion region1 = new MarketRegion();
		region1.setRegionCode( "US" );
		MarketRegion region2 = new MarketRegion();
		region2.setRegionCode( "EU" );
		
		Car car = new Car();
		car.setModel( "SUV" );
		car.setMarketRegion( region1 );
		
		SportCar car2 = new SportCar();
		car2.setModel( "350Z" );
		car2.setMarketRegion( region2 );
		
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( region1 );
		s.persist( region2 );
		s.persist( car );
		s.persist( car2 );
		
		s.flush();
		
		assertEquals( 1, s.createQuery( "select car from Car car where car.marketRegion.regionCode='US'")
				.list().size() );
		assertEquals( 1, s.createQuery( "select car from SportCar car where car.marketRegion.regionCode='EU'")
				.list().size() );
		
		tx.rollback();
		s.close();

	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Car.class,
				SportCar.class,
				MarketRegion.class
		};
	}
}
