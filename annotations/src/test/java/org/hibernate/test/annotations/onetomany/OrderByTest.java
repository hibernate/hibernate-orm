//$Id$
package org.hibernate.test.annotations.onetomany;

import org.hibernate.test.annotations.TestCase;
import org.hibernate.Session;

/**
 * @author Emmanuel Bernard
 */
public class OrderByTest extends TestCase {
	public void testOrderByOnIdClassProperties() throws Exception {
		Session s = openSession( );
		s.getTransaction().begin();
		Order o = new Order();
		o.setAcademicYear( 2000 );
		o.setSchoolId( "Supelec" );
		o.setSchoolIdSort( 1 );
		s.persist( o );
		OrderItem oi1 = new OrderItem();
		oi1.setAcademicYear( 2000 );
		oi1.setDayName( "Monday" );
		oi1.setSchoolId( "Supelec" );
		oi1.setOrder( o );
		oi1.setDayNo( 23 );
		s.persist( oi1 );
		OrderItem oi2 = new OrderItem();
		oi2.setAcademicYear( 2000 );
		oi2.setDayName( "Tuesday" );
		oi2.setSchoolId( "Supelec" );
		oi2.setOrder( o );
		oi2.setDayNo( 30 );
		s.persist( oi2 );
		s.flush();
		s.clear();

		OrderID oid = new OrderID();
		oid.setAcademicYear( 2000 );
		oid.setSchoolId( "Supelec" );
		o = (Order) s.get( Order.class, oid );
		assertEquals( 30, o.getItemList().get( 0 ).getDayNo().intValue() );

		s.getTransaction().rollback();
		s.close();
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Order.class,
				OrderItem.class
		};
	}
}
