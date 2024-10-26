/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@DomainModel(annotatedClasses = {
		SelectJoinedAssociationMultipleTimesTest.Book.class
})
@SessionFactory
public class SelectJoinedAssociationMultipleTimesTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// Add a proxy first to trigger the error
					session.getReference( Book.class, 1 );
					session.createSelectionQuery( "select b b1, b b2 from Book b", Object[].class ).getResultList();
				}
		);
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.persist( new Book( 1, "First book" ) ) );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.createMutationQuery( "delete Book" ).executeUpdate() );
	}

	@Entity( name = "Book")
	public static class Book {
		@Id
		private Integer id;
		private String name;

		public Book() {
		}

		public Book(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
