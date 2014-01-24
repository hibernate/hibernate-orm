/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.test.integration.customtype;

import org.hibernate.Session;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue( jiraKey = "HHH-8602" )
public class DefaultTypeTest extends BaseEnversFunctionalTestCase {
	private Long id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { CalendarEntity.class };
	}

	@Override
	protected String[] getAnnotatedPackages() {
		return new String[] { "org.hibernate.envers.test.integration.customtype" };
	}

	@Test
	@Priority(10)
	public void initData() {
		Session session = openSession();

		// Revision 1
		session.getTransaction().begin();
		final CalendarEntity entity = new CalendarEntity();
		entity.getDayOne().set( 2013, java.util.Calendar.DECEMBER, 24, 0, 0, 0 );
		entity.getDayTwo().set( 2013, java.util.Calendar.DECEMBER, 24, 0, 0, 0 );
		session.persist( entity );
		session.getTransaction().commit();

		id = entity.getId();

		session.close();
	}

	@Test
	public void testDateEqualityWithCurrentObject() {
		final CalendarEntity ver1 = getAuditReader().find( CalendarEntity.class, id, 1 );
		final Session session = openSession();
		final CalendarEntity current = (CalendarEntity) session.get( CalendarEntity.class, id );
		session.close();

		Assert.assertEquals( current.getDayOne(), ver1.getDayOne() );
		Assert.assertEquals( current.getDayTwo(), ver1.getDayTwo() );
	}
}
