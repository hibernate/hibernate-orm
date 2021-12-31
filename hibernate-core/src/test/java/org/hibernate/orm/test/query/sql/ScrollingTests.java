/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sql;

import java.math.BigInteger;

import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey( "HHH-11033" )
@DomainModel( annotatedClasses = ScrollingTests.MyEntity.class )
@SessionFactory
public class ScrollingTests {

	@Test
	public void testSimpleScrolling(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Query query = session.createNativeQuery( "select e.big from MY_ENTITY e" );
			try (ScrollableResults scroll = query.scroll()) {
				while ( scroll.next() ) {
					assertThat( scroll.get() ).isNotNull();
				}
			}
		} );
	}
	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( (session) -> {
			session.save( new MyEntity( 1L, "entity_1", new BigInteger( "3" ) ) );
			session.save( new MyEntity( 2L, "entity_2", new BigInteger( "6" ) ) );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( (session) -> session.createQuery( "delete MyEntity" ).executeUpdate() );
	}

	@Entity(name = "MyEntity")
	@Table(name = "MY_ENTITY")
	public static class MyEntity {
		@Id
		private Long id;

		private BigInteger big;

		private String description;

		public MyEntity() {
		}

		public MyEntity(Long id, String description, BigInteger big) {
			this.id = id;
			this.description = description;
			this.big = big;
		}

		public String getDescription() {
			return description;
		}
	}
}
