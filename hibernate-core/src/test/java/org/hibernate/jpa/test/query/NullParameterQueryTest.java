/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 * @see <a href="https://hibernate.atlassian.net/browse/JPA-31">JPA-31</a>
 */
@TestForIssue(jiraKey = "JPA-31")
public class NullParameterQueryTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Event.class
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Event event = new Event();

			entityManager.persist( event );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Event event = entityManager.createQuery(
					"select e " +
							"from Event e " +
							"where (:name is null or e.name = :name)", Event.class )
					.setParameter( "name", null )
					.getSingleResult();

			assertNotNull( event );
		} );
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		@GeneratedValue
		private Long id;

		private String name;
	}
}
