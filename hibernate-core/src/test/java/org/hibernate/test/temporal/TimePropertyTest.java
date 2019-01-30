/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.temporal;

import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.junit.Test;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * @author Gail Badner
 */
public class TimePropertyTest extends BaseCoreFunctionalTestCase {
	private final DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

	// NuoDB: add logger
	Logger logger = Logger.getLogger(BaseCoreFunctionalTestCase.class.getName());

	@Test
	public void testTimeAsDate() {
		final Entity eOrig = new Entity();

		// NuoDB: Modified to use original time in queries not the time in
		// eGotten. They are different - testTime has no milliseconds
		// but eGotten.tAsDate does. NuoDB query ignores millis so it
		// doesn't find a match.
		// Exact match times are probably as bogus as exact match floating
		// point numbers so this is reasonable behaviour.  The assert at
		// line 68 ignores millis but the queries do not.
		Time testTime = new Time( new Date().getTime() );

		eOrig.tAsDate = testTime;

		Session s = openSession();
		s.getTransaction().begin();
		s.persist( eOrig );
		s.getTransaction().commit();
		s.close();


		s = openSession();
		s.getTransaction().begin();
		final Entity eGotten = (Entity) s.get( Entity.class, eOrig.id );
		// Some databases retain the milliseconds when being inserted and some don't;
		final String tAsDateOrigFormatted = timeFormat.format( eOrig.tAsDate );
		final String tAsDateGottenFormatted = timeFormat.format( eGotten.tAsDate );
		logger.info("orig = " + eOrig.tAsDate + " gotten = " + eGotten.tAsDate );
		assertEquals( tAsDateOrigFormatted, tAsDateGottenFormatted );

		// NuoDB - check what's actually in table
		Time t = (Time)session.createNativeQuery("SELECT tAsDate FROM Entity WHERE id = 1").getSingleResult();
		logger.info("T is a " + t.getClass());
		assertEquals(t, testTime);
		logger.info("value in table is " + t);
		logger.info("t = " + t.getTime() + " eGotten.tAsDate = " + eGotten.tAsDate.getTime());
		// End NuoDB


		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		String queryString = "from TimePropertyTest$Entity where tAsDate = ?1";

		if( SQLServerDialect.class.isAssignableFrom( getDialect().getClass() )) {
			queryString = "from TimePropertyTest$Entity where tAsDate = cast ( ?1 as time )";
		}

		// NuoDB: replaced eGotten.tAsDate by testTime to fix query
		final Query queryWithParameter = s.createQuery( queryString ).setParameter( 1, testTime );
		final Entity eQueriedWithParameter = (Entity) queryWithParameter.uniqueResult();
		assertNotNull( eQueriedWithParameter );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();

		// NuoDB: replaced eGotten.tAsDate by testTime to fix query
		final Query query = s.createQuery( queryString ).setTime( 1, testTime );
		final Entity eQueried = (Entity) query.uniqueResult();
		assertNotNull( eQueried );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		s.delete( eQueried );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Entity.class };
	}

	@javax.persistence.Entity
	@Table(name = "entity")
	public static class Entity {
		@GeneratedValue
		@Id
		private long id;

		@Temporal( value = TemporalType.TIME )
		private java.util.Date tAsDate;
	}
}
