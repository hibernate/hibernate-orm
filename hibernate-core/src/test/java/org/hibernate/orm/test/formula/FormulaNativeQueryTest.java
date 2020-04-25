/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.formula;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Formula;
import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;
import static org.junit.Assert.assertThat;

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
	@FailureExpected( jiraKey = "HHH-7525", reason = "native query not implemented yet")
	void testNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Query<Foo> query = session.createNativeQuery( "SELECT ft.* FROM foo_table ft", Foo.class );
			final List<Foo> list = query.getResultList();
			assertThat( list, hasSize( 3 ) );
		} );
	}

	@Test
	void testHql(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Query<Foo> query = session.createQuery( "SELECT ft FROM Foo ft", Foo.class );
			final List<Foo> list = query.getResultList();
			assertThat( list, hasSize( 3 ) );
		} );
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

		public int getLocationStart() {
			return locationStart;
		}
		public void setLocationStart(int locationStart) {
			this.locationStart = locationStart;
		}

		public int getLocationEnd() {
			return locationEnd;
		}
		public void setLocationEnd(int locationEnd) {
			this.locationEnd = locationEnd;
		}

		@Formula("abs(locationEnd - locationStart)")
		public int getDistance() {
			return distance;
		}

		public void setDistance(int distance) {
			this.distance = distance;
		}
	}
}
