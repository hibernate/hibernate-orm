/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.filter.subclass.MappedSuperclass;

import java.util.List;

import org.hibernate.Transaction;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
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
		doInHibernate( this::sessionFactory, session -> {
			Mammal mammal = new Mammal();
			mammal.setName( "unimportant" );
			session.persist( mammal );

			Human human = new Human();
			human.setName( "unimportant" );
			session.persist( human );

			Human human1 = new Human();
			human1.setName( "unimportant_1" );
			session.persist( human1 );
		} );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8895")
	public void testSelectFromHuman() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			session.enableFilter( "nameFilter" ).setParameter( "name", "unimportant" );

			List humans = session.createQuery( "SELECT h FROM Human h" ).list();

			assertThat( humans.size(), is( 1 ) );
			Human human = (Human) humans.get( 0 );
			assertThat( human.getName(), is( "unimportant" ) );
		} );
	}
}
