//$Id$
package org.hibernate.test.annotations;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.StaleStateException;
import org.hibernate.Transaction;

/**
 * @author Emmanuel Bernard
 */
public class EntityTest extends TestCase {
	private DateFormat df;
	
	public EntityTest(String x) {
		super( x );
		df = SimpleDateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
	}

	public void testLoad() throws Exception {
		//put an object in DB
		assertEquals( "Flight", getCfg().getClassMapping( Flight.class.getName() ).getTable().getName() );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Flight firstOne = new Flight();
		firstOne.setId( new Long( 1 ) );
		firstOne.setName( "AF3202" );
		firstOne.setDuration( new Long( 1000000 ) );
		firstOne.setDurationInSec( 2000 );
		s.save( firstOne );
		s.flush();
		tx.commit();
		s.close();

		//read it
		s = openSession();
		tx = s.beginTransaction();
		firstOne = (Flight) s.get( Flight.class, new Long( 1 ) );
		assertNotNull( firstOne );
		assertEquals( new Long( 1 ), firstOne.getId() );
		assertEquals( "AF3202", firstOne.getName() );
		assertEquals( new Long( 1000000 ), firstOne.getDuration() );
		assertFalse( "Transient is not working", 2000l == firstOne.getDurationInSec() );
		tx.commit();
		s.close();
	}

	public void testColumn() throws Exception {
		//put an object in DB
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Flight firstOne = new Flight();
		firstOne.setId( new Long( 1 ) );
		firstOne.setName( "AF3202" );
		firstOne.setDuration( new Long( 1000000 ) );
		firstOne.setDurationInSec( 2000 );
		s.save( firstOne );
		s.flush();
		tx.commit();
		s.close();
		

		s = openSession();
		tx = s.beginTransaction();
		firstOne = new Flight();
		firstOne.setId( new Long( 1 ) );
		firstOne.setName( null );

		try {
			s.save( firstOne );
			tx.commit();
			fail( "Name column should be not null" );
		}
		catch (HibernateException e) {
			//fine
		}
		finally {
			s.close();
		}

		//insert an object and check that name is not updatable
		s = openSession();
		tx = s.beginTransaction();
		firstOne = new Flight();
		firstOne.setId( new Long( 1 ) );
		firstOne.setName( "AF3202" );
		firstOne.setTriggeredData( "should not be insertable" );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		firstOne = (Flight) s.get( Flight.class, new Long( 1 ) );
		assertNotNull( firstOne );
		assertEquals( new Long( 1 ), firstOne.getId() );
		assertEquals( "AF3202", firstOne.getName() );
		assertFalse( "should not be insertable".equals( firstOne.getTriggeredData() ) );
		firstOne.setName( "BA1234" );
		firstOne.setTriggeredData( "should not be updatable" );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		firstOne = (Flight) s.get( Flight.class, new Long( 1 ) );
		assertNotNull( firstOne );
		assertEquals( new Long( 1 ), firstOne.getId() );
		assertEquals( "AF3202", firstOne.getName() );
		assertFalse( "should not be updatable".equals( firstOne.getTriggeredData() ) );
		tx.commit();
		s.close();
	}

	public void testColumnUnique() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Sky sky = new Sky();
		sky.id = new Long( 2 );
		sky.color = "blue";
		sky.day = "monday";
		sky.month = "January";

		Sky sameSky = new Sky();
		sameSky.id = new Long( 3 );
		sameSky.color = "blue";
		sky.day = "tuesday";
		sky.month = "January";

		try {
			s.save( sky );
			s.flush();
			s.save( sameSky );
			tx.commit();
			fail( "unique constraints not respected" );
		}
		catch (HibernateException e) {
			//success
		}
		finally {
			if ( tx != null ) tx.rollback();
			s.close();
		}
	}

	public void testUniqueConstraint() throws Exception {
		int id = 5;
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Sky sky = new Sky();
		sky.id = new Long( id++ );
		sky.color = "green";
		sky.day = "monday";
		sky.month = "March";

		Sky otherSky = new Sky();
		otherSky.id = new Long( id++ );
		otherSky.color = "red";
		otherSky.day = "friday";
		otherSky.month = "March";

		Sky sameSky = new Sky();
		sameSky.id = new Long( id++ );
		sameSky.color = "green";
		sameSky.day = "monday";
		sameSky.month = "March";

		s.save( sky );
		s.flush();

		s.save( otherSky );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		try {
			s.save( sameSky );
			tx.commit();
			fail( "unique constraints not respected" );
		}
		catch (HibernateException e) {
			//success
		}
		finally {
			if ( tx != null ) tx.rollback();
			s.close();
		}
	}

	public void testVersion() throws Exception {
//		put an object in DB
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Flight firstOne = new Flight();
		firstOne.setId( new Long( 2 ) );
		firstOne.setName( "AF3202" );
		firstOne.setDuration( new Long( 500 ) );
		s.save( firstOne );
		s.flush();
		tx.commit();
		s.close();

		//read it
		s = openSession();
		tx = s.beginTransaction();
		firstOne = (Flight) s.get( Flight.class, new Long( 2 ) );
		tx.commit();
		s.close();

		//read it again
		s = openSession();
		tx = s.beginTransaction();
		Flight concurrentOne = (Flight) s.get( Flight.class, new Long( 2 ) );
		concurrentOne.setDuration( new Long( 1000 ) );
		s.update( concurrentOne );
		tx.commit();
		s.close();
		assertFalse( firstOne == concurrentOne );
		assertFalse( firstOne.getVersion().equals( concurrentOne.getVersion() ) );

		//reattach the first one
		s = openSession();
		tx = s.beginTransaction();
		firstOne.setName( "Second access" );
		s.update( firstOne );
		try {
			tx.commit();
			fail( "Optimistic locking should work" );
		}
		catch (StaleStateException e) {
			//fine
		}
		finally {
			if ( tx != null ) tx.rollback();
			s.close();
		}

	}

	public void testFieldAccess() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Sky sky = new Sky();
		sky.id = new Long( 1 );
		sky.color = "black";
		Sky.area = "Paris";
		sky.day = "23";
		sky.month = "1";
		s.save( sky );
		tx.commit();
		s.close();
		Sky.area = "London";

		s = openSession();
		tx = s.beginTransaction();
		sky = (Sky) s.get( Sky.class, sky.id );
		assertNotNull( sky );
		assertEquals( "black", sky.color );
		assertFalse( "Paris".equals( Sky.area ) );
		tx.commit();
		s.close();
	}

	public void testEntityName() throws Exception {
		assertEquals( "Corporation", getCfg().getClassMapping( Company.class.getName() ).getTable().getName() );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Company comp = new Company();
		s.persist( comp );
		comp.setName( "JBoss Inc" );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		List result = s.createQuery( "from Corporation" ).list();
		assertNotNull( result );
		assertEquals( 1, result.size() );
		tx.commit();
		s.close();

	}

	public void testNonGetter() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Flight airFrance = new Flight();
		airFrance.setId( new Long( 747 ) );
		airFrance.setName( "Paris-Amsterdam" );
		airFrance.setDuration( new Long( 10 ) );
		airFrance.setFactor( 25 );
		s.persist( airFrance );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		airFrance = (Flight) s.get( Flight.class, airFrance.getId() );
		assertNotNull( airFrance );
		assertEquals( new Long( 10 ), airFrance.getDuration() );
		assertFalse( 25 == airFrance.getFactor( false ) );
		s.delete( airFrance );
		tx.commit();
		s.close();
	}

	public void testTemporalType() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Flight airFrance = new Flight();
		airFrance.setId( new Long( 747 ) );
		airFrance.setName( "Paris-Amsterdam" );
		airFrance.setDuration( new Long( 10 ) );
		airFrance.setDepartureDate( new Date( 05, 06, 21, 10, 0, 0 ) );
		airFrance.setAlternativeDepartureDate( new GregorianCalendar( 2006, 02, 03, 10, 00 ) );
		airFrance.getAlternativeDepartureDate().setTimeZone( TimeZone.getTimeZone( "GMT" ) );
		airFrance.setBuyDate( new java.sql.Timestamp(122367443) );
		airFrance.setFactor( 25 );
		s.persist( airFrance );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Query q = s.createQuery( "from Flight f where f.departureDate = :departureDate" );
		q.setParameter( "departureDate", airFrance.getDepartureDate(), Hibernate.DATE );
		Flight copyAirFrance = (Flight) q.uniqueResult();
		assertNotNull( copyAirFrance );
		assertEquals(
				df.format(new Date( 05, 06, 21 )).toString(),
				df.format(copyAirFrance.getDepartureDate()).toString()
		);
		assertEquals( df.format(airFrance.getBuyDate()), df.format(copyAirFrance.getBuyDate()));

		s.delete( copyAirFrance );
		tx.commit();
		s.close();
	}

	public void testBasic() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Flight airFrance = new Flight();
		airFrance.setId( new Long( 747 ) );
		airFrance.setName( "Paris-Amsterdam" );
		airFrance.setDuration( null );
		try {
			s.persist( airFrance );
			tx.commit();
			fail( "Basic(optional=false) fails" );
		}
		catch (Exception e) {
			//success
			if ( tx != null ) tx.rollback();
		}
		finally {
			s.close();
		}
	}

	/**
	 * @see org.hibernate.test.annotations.TestCase#getAnnotatedClasses()
	 */
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Flight.class,
				Company.class,
				Sky.class
		};
	}

}

