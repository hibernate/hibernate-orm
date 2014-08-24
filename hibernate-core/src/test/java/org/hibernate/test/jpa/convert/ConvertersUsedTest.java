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
package org.hibernate.test.jpa.convert;

import static org.junit.Assert.*;

import org.hibernate.Session;
import org.hibernate.test.jpa.AbstractJPATest;
import org.hibernate.testing.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Checks that when persisting a {@code null} value and an attribute converter is configured, it is
 * called and its return value properly used.
 *
 * @author Etienne Miret
 */
public class ConvertersUsedTest extends AbstractJPATest {

	@Override
	public String[] getMappings() {
		return new String[0];
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Animal.class };
	}

	@Before
	public void insertData() {
		final Session s = openSession();
		s.getTransaction().begin();

		final Animal a = new Animal();
		s.persist( a );

		s.getTransaction().commit();
		s.close();
	}

	@After
	public void clearData() {
		final Session s = openSession();
		s.getTransaction().begin();

		s.createQuery( "delete from Animal" ).executeUpdate();

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testStoring() {
		final Session s = openSession();
		s.getTransaction().begin();

		final Animal a = (Animal) s.createQuery( "from Animal" ).list().get( 0 );
		assertNotNull( a.getIsFemale() );

		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9320" )
	public void testRetrieving() {
		final Session s = openSession();
		s.getTransaction().begin();

		final Animal a = (Animal) s.createQuery( "from Animal" ).list().get( 0 );
		assertNotNull( a.getDescription() );

		s.getTransaction().commit();
		s.close();
	}

}
