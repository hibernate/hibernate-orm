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

	@Test
	public void testTimeAsDate() {
		final Entity eOrig = new Entity();
		Calendar calendar = Calendar.getInstance();
		// See javadoc for java.sql.Time: 'The date components should be set to the "zero epoch" value of January 1, 1970 and should not be accessed'
		// Other dates can potentially lead to errors in JDBC drivers, in particular MySQL ConnectorJ 8.x.
		calendar.set( 1970, Calendar.JANUARY, 1 );
		eOrig.tAsDate = new Time( calendar.getTimeInMillis() );

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

		String queryString = "from TimePropertyTest$Entity where tAsDate = ?1";

		if( SQLServerDialect.class.isAssignableFrom( getDialect().getClass() )) {
			queryString = "from TimePropertyTest$Entity where tAsDate = cast ( ?1 as time )";
		}

		final Query queryWithParameter = s.createQuery( queryString ).setParameter( 1, eGotten.tAsDate );
		final Entity eQueriedWithParameter = (Entity) queryWithParameter.uniqueResult();
		assertNotNull( eQueriedWithParameter );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();

		final Query query = s.createQuery( queryString ).setTime( 1, eGotten.tAsDate );
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
