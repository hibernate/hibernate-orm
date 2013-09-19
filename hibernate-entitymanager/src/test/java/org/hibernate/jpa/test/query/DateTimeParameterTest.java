/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.query;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class DateTimeParameterTest extends BaseEntityManagerFunctionalTestCase {
	private static GregorianCalendar nowCal = new GregorianCalendar();
	private static Date now = new Date( nowCal.getTime().getTime() );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Thing.class };
	}

	@Test
	public void testBindingCalendarAsDate() {
		createTestData();

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		try {
			Query query = em.createQuery( "from Thing t where t.someDate = :aDate" );
			query.setParameter( "aDate", nowCal, TemporalType.DATE );
			List list = query.getResultList();
			assertEquals( 1, list.size() );
		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}

		deleteTestData();
	}

	private void createTestData() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( new Thing( 1, "test", now, now, now ) );
		em.getTransaction().commit();
		em.close();
	}

	private void deleteTestData() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete Thing" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

	@Entity( name="Thing" )
	@Table( name = "THING" )
	public static class Thing {
		@Id
		public Integer id;
		public String someString;
		@Temporal( TemporalType.DATE )
		public Date someDate;
		@Temporal( TemporalType.TIME )
		public Date someTime;
		@Temporal( TemporalType.TIMESTAMP )
		public Date someTimestamp;

		public Thing() {
		}

		public Thing(Integer id, String someString, Date someDate, Date someTime, Date someTimestamp) {
			this.id = id;
			this.someString = someString;
			this.someDate = someDate;
			this.someTime = someTime;
			this.someTimestamp = someTimestamp;
		}
	}
}
