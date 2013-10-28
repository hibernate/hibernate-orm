/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
