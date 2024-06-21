/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.formula;

import java.util.Collections;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Formula;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Алексей Макаров
 * @author Gail Badner
 * @author Nathan Xu
 */
@DomainModel( annotatedClasses = FormulaNativeQueryTest.Foo.class )
@SessionFactory
@TestForIssue(jiraKey = "HHH-7525")
public class FormulaNativeQueryTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Foo( 1, 1 ) );
			session.persist( new Foo( 1, 2 ) );
			session.persist( new Foo( 2, 1 ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createQuery( "delete from Foo" ).executeUpdate() );
	}

	@Test
	void testNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query<Foo> query = session.createNativeQuery( "SELECT ft.* FROM foo_table ft", Foo.class );
					try {
						query.getResultList();
						Assertions.fail(
								"the native query result mapping should fail if the formula field is not returned from the query" );
					}
					catch (Exception ex) {
						Assertions.assertTrue( ex.getMessage().contains( "distance" ) );
					}
				}
		);
	}

	@Test
	public void testNativeQueryWithAllFields(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query<Foo> query = session.createNativeQuery(
							"SELECT ft.*, abs(location_end - location_start) as distance FROM foo_table ft",
							Foo.class
					);
					List<Foo> list = query.getResultList();
					assertThat( list, hasSize( 3 ) );
				}
		);
	}

	@Test
	public void testNativeQueryWithAliasProperties(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					NativeQuery query = session.createNativeQuery(
							"SELECT ft.*, abs(ft.location_end - location_start) as d FROM foo_table ft" );
					query.addRoot( "ft", Foo.class )
							.addProperty( "id", "id" )
							.addProperty( "locationStart", "location_start" )
							.addProperty( "locationEnd", "location_end" )
							.addProperty( "distance", "d" );
					List<Foo> list = query.getResultList();
					assertThat( list, hasSize( 3 ) );
				}
		);
	}

	@Test
	public void testNativeQueryWithAliasSyntax(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					NativeQuery query = session.createNativeQuery(
							"SELECT ft.id as {ft.id}, ft.location_start as {ft.locationStart}, ft.location_end as {ft.locationEnd}, abs(ft.location_end - location_start) as {ft.distance} FROM foo_table ft" )
							.addEntity( "ft", Foo.class );
					query.setProperties( Collections.singletonMap( "distance", "distance" ) );
					List<Foo> list = query.getResultList();
					assertThat( list, hasSize( 3 ) );
				}
		);
	}

	@Test
	void testHql(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query<Foo> query = session.createQuery( "SELECT ft FROM Foo ft", Foo.class );
					final List<Foo> list = query.getResultList();
					assertThat( list, hasSize( 3 ) );
				}
		);
	}

	@Entity(name = "Foo")
	@Table(name = "foo_table")
	public static class Foo {

		private int id;
		private int locationStart;
		private int locationEnd;
		private int distance;

		public Foo() {
		}

		public Foo(int locationStart, int locationEnd) {
			this.locationStart = locationStart;
			this.locationEnd = locationEnd;
		}

		@Id
		@GeneratedValue
		public int getId() {
			return id;
		}
		private void setId(int id) {
			this.id = id;
		}

		@Column(name = "location_start")
		public int getLocationStart() {
			return locationStart;
		}
		public void setLocationStart(int locationStart) {
			this.locationStart = locationStart;
		}

		@Column(name = "location_end")
		public int getLocationEnd() {
			return locationEnd;
		}
		public void setLocationEnd(int locationEnd) {
			this.locationEnd = locationEnd;
		}

		@Formula("abs(location_end - location_start)")
		public int getDistance() {
			return distance;
		}

		public void setDistance(int distance) {
			this.distance = distance;
		}
	}
}
