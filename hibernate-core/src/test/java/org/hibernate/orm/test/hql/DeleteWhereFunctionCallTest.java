/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hql;

import org.hibernate.Session;
import org.hibernate.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				DeleteWhereFunctionCallTest.SuperType.class,
				DeleteWhereFunctionCallTest.SubType.class
		}
)
@SessionFactory
public class DeleteWhereFunctionCallTest {

	@BeforeEach
	public void initData(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.persist( new SuperType( -1 ) );
			s.persist( new SubType( -2 ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from supert" ).executeUpdate();
					session.createQuery( "delete from subt" ).executeUpdate();
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14814")
	public void testDeleteWhereTypeFunctionCall(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			assertThat( count( s, SuperType.class ) ).isEqualTo( 2 );
			assertThat( count( s, SubType.class ) ).isEqualTo( 1 );
		} );
		scope.inTransaction( s -> {
			Query<?> query = s.createQuery( "delete from " + SuperType.class.getName() + " e"
													+ " where type( e ) = :type" );
			query.setParameter( "type", SuperType.class );
			query.executeUpdate();
		} );
		scope.inTransaction( s -> {
			assertThat( count( s, SuperType.class ) ).isEqualTo( 1 );
			assertThat( count( s, SubType.class ) ).isEqualTo( 1 );
		} );
	}

	@Test
	public void testDeleteWhereAbsFunctionCall(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			assertThat( count( s, SuperType.class ) ).isEqualTo( 2 );
			assertThat( count( s, SubType.class ) ).isEqualTo( 1 );
		} );
		scope.inTransaction( s -> {
			Query<?> query = s.createQuery( "delete from " + SuperType.class.getName() + " e"
													+ " where abs( e.someNumber ) = :number" );
			query.setParameter( "number", 2 );
			query.executeUpdate();
		} );
		scope.inTransaction( s -> {
			assertThat( count( s, SuperType.class ) ).isEqualTo( 1 );
			assertThat( count( s, SubType.class ) ).isEqualTo( 0 );
		} );
	}

	private <T> long count(Session s, Class<T> entityClass) {
		return s.createQuery( "select count(e) from " + entityClass.getName() + " e", Long.class )
				.uniqueResult();
	}

	@Entity(name = "supert")
	public static class SuperType {
		@Id
		@GeneratedValue
		private Long id;

		private int someNumber;

		public SuperType() {
		}

		public SuperType(int someNumber) {
			this.someNumber = someNumber;
		}
	}

	@Entity(name = "subt")
	public static class SubType extends SuperType {
		public SubType() {
		}

		public SubType(int someNumber) {
			super( someNumber );
		}
	}
}
