/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.temporal;

import java.util.Calendar;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Query;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
public class TemporalTypeTest extends BaseEntityManagerFunctionalTestCase {
	
	@Test
	public void testTemporalType() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		
		Date date = new Date();
		Calendar calendar = Calendar.getInstance();
		
		DataPoint dp = new DataPoint();
		dp.date1 = date;
		dp.date2 = date;
		dp.calendar1 = calendar;
		dp.calendar2 = calendar;
		em.persist( dp );
		
		em.getTransaction().commit();
		em.close();

		doTest("date1", date);
		doTest("date1", calendar);
		doTest("date2", date);
		doTest("date2", calendar);

		doTest("calendar1", date);
		doTest("calendar1", calendar);
		doTest("calendar2", date);
		doTest("calendar2", calendar);
	}
	
	private void doTest(String property, Object obj) {
		doTest( property, obj, TemporalType.DATE );
		doTest( property, obj, TemporalType.TIMESTAMP );
	}
	
	private void doTest(String property, Object obj, TemporalType temporalType) {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		
		Query query = em.createQuery("from DataPoint where " + property + " = :obj");
		if (obj instanceof Calendar) {
			query.setParameter("obj", (Calendar) obj, temporalType);
		}
		else {
			query.setParameter("obj", (Date) obj, temporalType);
		}
		
		em.getTransaction().commit();
		em.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { DataPoint.class };
	}

	@Entity(name = "DataPoint")
	private static class DataPoint {
		@Id @GeneratedValue
		public long id;
		
		@Temporal( TemporalType.DATE )
		public Date date1;
		
		@Temporal( TemporalType.TIMESTAMP )
		public Date date2;
		
		@Temporal( TemporalType.DATE )
		public Calendar calendar1;
		
		@Temporal( TemporalType.TIMESTAMP )
		public Calendar calendar2;
	}
}
