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
import java.util.Date;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.junit.Test;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * @author Gail Badner
 */
public class TimePropertyTest extends BaseCoreFunctionalTestCase {
	private final DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

	@Test
	public void testTimeAsDate() {
		final Entity eOrig = new Entity();
		eOrig.tAsDate = new Time( new Date().getTime() );

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
		assertEquals( tAsDateOrigFormatted, tAsDateGottenFormatted );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		final Query queryWithParameter = s.createQuery( "from TimePropertyTest$Entity where tAsDate=?" ).setParameter( 0, eOrig.tAsDate );
		final Entity eQueriedWithParameter = (Entity) queryWithParameter.uniqueResult();
		assertNotNull( eQueriedWithParameter );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		final Query query = s.createQuery( "from TimePropertyTest$Entity where tAsDate=?" ).setTime( 0, eOrig.tAsDate );
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
	public static class Entity {
		@GeneratedValue
		@Id
		private long id;

		@Temporal( value = TemporalType.TIME )
		private java.util.Date tAsDate;
	}
}
