/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @implNote Skipped for Dialects which do not support at lease
 */
@JiraKey(value = "HHH-15895")
@DomainModel(annotatedClasses = InPredicateTest.Event.class)
@SessionFactory(exportSchema = false)
public class InPredicateTest {

	@Test
	public void testInPredicate(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Event> cr = cb.createQuery( Event.class );
			Root<Event> root = cr.from( Event.class );
			List<String> names = getNames();
			cr.select( root ).where( root.get( "name" ).in( names ) );

			// This should trigger the error from HHH-15895 as QuerySqmImpl
			// tries to handle the Criteria parameters
			session.createQuery( cr );
		} );
	}

	private List<String> getNames() {
		int maxNames = 100000;
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
