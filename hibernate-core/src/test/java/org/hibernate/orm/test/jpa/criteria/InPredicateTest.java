/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Jpa(annotatedClasses = {
		InPredicateTest.Event.class
})
@TestForIssue(jiraKey = "HHH-15895")
public class InPredicateTest {

	@Test
	public void testInPredicate(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<Event> cr = cb.createQuery( Event.class );
					Root<Event> root = cr.from( Event.class );
					List<String> names = getNames( scope );
					cr.select( root ).where( root.get( "name" ).in( names ) );

					List<Event> results = entityManager.createQuery( cr ).getResultList();
					assertNotNull( results );
				}
		);
	}

	private List<String> getNames(EntityManagerFactoryScope scope) {
		int maxNames;
		Dialect dialect = scope.getDialect();
		if ( dialect instanceof H2Dialect
				|| dialect instanceof MariaDBDialect
				|| dialect instanceof HSQLDialect
				|| dialect instanceof MySQLDialect ) {
			maxNames = 100000;
		}
		else {
			// the other dialects does not support 100000 parameters
			maxNames = 65500;
		}

		List<String> names = new ArrayList<>( maxNames );
		for ( int i = 0; i < maxNames; i++ ) {
			names.add( "abc" + i );
		}
		return names;
	}

	@Entity(name = "Event")
	@Table(name = "EVENT_TABLE")
	public static class Event {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Event() {
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

	}
}
