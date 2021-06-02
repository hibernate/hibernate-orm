/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author Jeremy Carnus
 * @author Guillaume Smet
 */
@TestForIssue(jiraKey = "HHH-12989")
@Jpa( annotatedClasses = InWithHeterogeneousCollectionTest.Event.class )
public class InWithHeterogeneousCollectionTest {

	@Test
	@RequiresDialect(H2Dialect.class)
	void testCaseClause(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder cb = em.getCriteriaBuilder();

			CriteriaQuery<Event> criteria = cb.createQuery( Event.class );

			Root<Event> eventRoot = criteria.from( Event.class );
			Path<String> namePath = eventRoot.get( "name" );
			Path<String> tagPath = eventRoot.get( "tag" );

			Expression<String> expression = cb.function(
					"lower",
					String.class,
					namePath );

			criteria.select( eventRoot );
			criteria.where( tagPath.in( Arrays.asList( expression, "my-tag" ) ) );
			List<Event> resultList = em.createQuery( criteria ).getResultList();

			assertThat( resultList, hasSize( 2 ) );
		} );
	}

	@BeforeEach
	void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new Event( 1L, "EventName1", "EventName1".toLowerCase( Locale.ROOT ) ) );
			em.persist( new Event( 2L, "EventName2", "my-tag" ) );
		} );
	}

	@AfterEach
	void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			em.createQuery( "delete Event" ).executeUpdate();
		} );
	}

	@Entity(name = "Event")
	static class Event {

		@Id
		private Long id;

		@Column
		private String name;

		@Column
		private String tag;

		protected Event() {
		}

		public Event(Long id, String name, String tag) {
			this.id = id;
			this.name = name;
			this.tag = tag;
		}

		public String getName() {
			return name;
		}

		public String getTag() {
			return tag;
		}
	}
}
