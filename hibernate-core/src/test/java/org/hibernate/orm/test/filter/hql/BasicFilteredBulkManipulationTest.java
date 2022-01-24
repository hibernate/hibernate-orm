/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.filter.hql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for application of filters
 *
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				BasicFilteredBulkManipulationTest.Person.class
		}
)
@SessionFactory
public class BasicFilteredBulkManipulationTest {

	@Test
	void testBasicFilteredHqlDelete(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.save( new Person( "Steve", 'M' ) );
			session.save( new Person( "Emmanuel", 'M' ) );
			session.save( new Person( "Gail", 'F' ) );
		} );
		scope.inTransaction( session -> {
			session.enableFilter( "sex" ).setParameter( "sexCode", 'M' );
			int count = session.createQuery( "delete Person" ).executeUpdate();
			assertEquals( 2, count );
		} );
	}

	@Test
	void testBasicFilteredHqlUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.save( new Person( "Shawn", 'M' ) );
			session.save( new Person( "Sally", 'F' ) );
		} );

		scope.inTransaction( session -> {
			session.enableFilter( "sex" ).setParameter( "sexCode", 'M' );
			int count = session.createQuery( "update Person p set p.name = 'Shawn'" ).executeUpdate();
			assertEquals( 1, count );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createQuery( "delete Person" ).executeUpdate() );
	}

	@Entity( name = "Person" )
	@FilterDef(
			name = "sex",
			parameters = @ParamDef(
				name= "sexCode",
				type = Character.class
			)
	)
	@Filter( name = "sex", condition = "SEX_CODE = :sexCode" )
	public static class Person {

		@Id @GeneratedValue
		private Long id;

		private String name;

		@Column( name = "SEX_CODE" )
		private char sex;

		protected Person() {
		}

		public Person(String name, char sex) {
			this.name = name;
			this.sex = sex;
		}
	}
}
