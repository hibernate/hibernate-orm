/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

public class DeleteWhereFunctionCallTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				SuperType.class,
				SubType.class
		};
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Before
	public void initData() {
		inTransaction( s -> {
			s.persist( new SuperType( -1 ) );
			s.persist( new SubType( -2 ) );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14814")
	public void testDeleteWhereTypeFunctionCall() {
		inTransaction( s -> {
			assertThat( count( s, SuperType.class ) ).isEqualTo( 2 );
			assertThat( count( s, SubType.class ) ).isEqualTo( 1 );
		} );
		inTransaction( s -> {
			Query<?> query = s.createQuery( "delete from " + SuperType.class.getName() + " e"
					+ " where type( e ) = :type" );
			query.setParameter( "type", SuperType.class );
			query.executeUpdate();
		} );
		inTransaction( s -> {
			assertThat( count( s, SuperType.class ) ).isEqualTo( 1 );
			assertThat( count( s, SubType.class ) ).isEqualTo( 1 );
		} );
	}

	@Test
	public void testDeleteWhereAbsFunctionCall() {
		inTransaction( s -> {
			assertThat( count( s, SuperType.class ) ).isEqualTo( 2 );
			assertThat( count( s, SubType.class ) ).isEqualTo( 1 );
		} );
		inTransaction( s -> {
			Query<?> query = s.createQuery( "delete from " + SuperType.class.getName() + " e"
					+ " where abs( e.someNumber ) = :number" );
			query.setParameter( "number", 2 );
			query.executeUpdate();
		} );
		inTransaction( s -> {
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
