/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) {DATE}, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.filter.subclass.MappedSuperclass;

import java.util.List;

import org.hibernate.Transaction;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */

public class FilterInheritanceTest extends BaseCoreFunctionalTestCase {
	private Transaction transaction;

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {Animal.class, Human.class, Mammal.class};
	}

	@Override
	protected void prepareTest() throws Exception {
		openSession();
		session.beginTransaction();

		persistTestData();

		session.getTransaction().commit();
		session.close();
	}

	@Override
	protected void cleanupTest() throws Exception {
		super.cleanupTest();
		openSession();
		session.beginTransaction();

		session.createQuery( "delete from Human" ).executeUpdate();
		session.createQuery( "delete from Mammal" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	protected void persistTestData() {
		Mammal mammal = new Mammal();
		mammal.setName( "unimportant" );
		session.persist( mammal );

		Human human = new Human();
		human.setName( "unimportant" );
		session.persist( human );

		Human human1 = new Human();
		human1.setName( "unimportant_1" );
		session.persist( human1 );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8895")
	public void testSlectFromHuman() throws Exception {
		openSession();
		session.enableFilter( "nameFilter" ).setParameter( "name", "unimportant" );
		try {
			transaction = session.beginTransaction();
			List humans = session.createQuery( "SELECT h FROM Human h" ).list();

			assertThat( humans.size(), is( 1 ) );
			Human human = (Human) humans.get( 0 );
			assertThat(human.getName(), is("unimportant"));

			transaction.commit();
		}
		catch (Exception e) {
			if ( transaction != null ) {
				transaction.rollback();
			}
			throw e;
		}
	}
}
