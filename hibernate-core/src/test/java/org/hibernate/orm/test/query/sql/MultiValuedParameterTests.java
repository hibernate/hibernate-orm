/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sql;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.NotImplementedYet;
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
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = MultiValuedParameterTests.MyEntity.class )
@SessionFactory
public class MultiValuedParameterTests {
	@Test
	public void testSetParameterList(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final List<String> descriptions = new ArrayList<>();
			descriptions.add( "a" );
			descriptions.add( "b" );
			final Query query = session.createNativeQuery( "select e.description from t_entity e where e.description in (:descriptions)" )
					.setParameterList( "descriptions", descriptions );
			try ( ScrollableResults scroll = query.scroll()) {
				while ( scroll.next() ) {
					assertThat( scroll.get() ).isNotNull();
				}
			}
		} );
	}

	@Test
	@NotImplementedYet( strict = false, reason = "#setParameter with a Collection does not work" )
	public void testSetParameter(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final List<String> descriptions = new ArrayList<>();
			descriptions.add( "a" );
			descriptions.add( "b" );
			final Query query = session.createNativeQuery( "select e.description from t_entity e where e.description in (:descriptions)" )
					.setParameter( "descriptions", descriptions );
			try ( ScrollableResults scroll = query.scroll()) {
				while ( scroll.next() ) {
					assertThat( scroll.get() ).isNotNull();
				}
			}
		} );
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.persist( new MyEntity( 1, "a" ) ) );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.createQuery( "delete MyEntity" ).executeUpdate() );
	}

	@Entity(name = "MyEntity")
	@Table(name = "t_entity")
	public static class MyEntity {
		@Id
		private Integer id;
		private String description;

		public MyEntity() {
		}

		public MyEntity(Integer id, String description) {
			this.id = id;
			this.description = description;
		}
	}
}
