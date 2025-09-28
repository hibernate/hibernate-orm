/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.assignment;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.query.assignment.Assignment;
import org.hibernate.query.restriction.Path;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.specification.DeleteSpecification;
import org.hibernate.query.specification.SelectionSpecification;
import org.hibernate.query.specification.SimpleProjectionSpecification;
import org.hibernate.query.specification.UpdateSpecification;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("JUnitMalformedDeclaration")
@SessionFactory
@DomainModel(annotatedClasses = AssignmentTest.Book.class)
public class AssignmentTest {

	@BeforeEach
	void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist(new Book("9781932394153", "Hibernate in Action"));
			session.persist(new Book("9781617290459", "Java Persistence with Hibernate"));
		});
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test void testAssignment(SessionFactoryScope scope) {
		var bookType = scope.getSessionFactory().getJpaMetamodel().findEntityType(Book.class);
		assertNotNull(  bookType );
		@SuppressWarnings("unchecked")
		var title =
				(SingularAttribute<? super Book, String>)
						bookType.findSingularAttribute("title");
		@SuppressWarnings("unchecked")
		var isbn =
				(SingularAttribute<? super Book, String>)
						bookType.findSingularAttribute("isbn");
		scope.inTransaction(session -> {
			int rows =
					UpdateSpecification.create( Book.class )
							.assign( Assignment.set( title, "Hibernate In Action!" ) )
							.restrict( Restriction.equal( isbn, "9781932394153" ) )
							.createQuery( session )
							.executeUpdate();
			assertEquals( 1, rows );
			assertEquals( "Hibernate In Action!",
					session.find( Book.class, "9781932394153" ).title );
		});
	}

	@Test void testPathAssignment(SessionFactoryScope scope) {
		var bookType = scope.getSessionFactory().getJpaMetamodel().findEntityType(Book.class);
		assertNotNull(  bookType );
		@SuppressWarnings("unchecked")
		var title =
				(SingularAttribute<? super Book, String>)
						bookType.findSingularAttribute("title");
		@SuppressWarnings("unchecked")
		var isbn =
				(SingularAttribute<? super Book, String>)
						bookType.findSingularAttribute("isbn");
		scope.inTransaction(session -> {
			int rows =
					UpdateSpecification.create( Book.class )
							.assign( Assignment.set( Path.from(Book.class).to(title),
									"Hibernate In Action!!!" ) )
							.restrict( Restriction.equal( isbn, "9781932394153" ) )
							.createQuery( session )
							.executeUpdate();
			assertEquals( 1, rows );
			assertEquals( "Hibernate In Action!!!",
					session.find( Book.class, "9781932394153" ).title );
		});
	}

	@Test void testAssignmentCriteria(SessionFactoryScope scope) {
		var bookType = scope.getSessionFactory().getJpaMetamodel().findEntityType(Book.class);
		assertNotNull(  bookType );
		@SuppressWarnings("unchecked")
		var title =
				(SingularAttribute<? super Book, String>)
						bookType.findSingularAttribute("title");
		@SuppressWarnings("unchecked")
		var isbn =
				(SingularAttribute<? super Book, String>)
						bookType.findSingularAttribute("isbn");
		scope.inTransaction(session -> {
			var builder = session.getCriteriaBuilder();
			var query = builder.createCriteriaUpdate( Book.class );
			var root = query.from( Book.class );
			query.where( root.get( isbn ).equalTo( "9781932394153" ) );
			int rows =
					UpdateSpecification.create( query )
							.assign( Assignment.set( title, "Hibernate in Action!" ) )
							.createQuery( session )
							.executeUpdate();
			assertEquals( 1, rows );
			assertEquals( "Hibernate in Action!",
					session.find( Book.class, "9781932394153" ).title );
		});
	}

	@Test void testPathAssignmentCriteria(SessionFactoryScope scope) {
		var bookType = scope.getSessionFactory().getJpaMetamodel().findEntityType(Book.class);
		assertNotNull(  bookType );
		@SuppressWarnings("unchecked")
		var title =
				(SingularAttribute<? super Book, String>)
						bookType.findSingularAttribute("title");
		@SuppressWarnings("unchecked")
		var isbn =
				(SingularAttribute<? super Book, String>)
						bookType.findSingularAttribute("isbn");
		scope.inTransaction(session -> {
			var builder = session.getCriteriaBuilder();
			var query = builder.createCriteriaUpdate( Book.class );
			var root = query.from( Book.class );
			query.where( root.get( isbn ).equalTo( "9781932394153" ) );
			int rows =
					UpdateSpecification.create( query )
							.assign( Assignment.set( Path.from(Book.class).to(title),
									"Hibernate in Action!!!" ) )
							.createQuery( session )
							.executeUpdate();
			assertEquals( 1, rows );
			assertEquals( "Hibernate in Action!!!",
					session.find( Book.class, "9781932394153" ).title );
		});
	}

	@Test void testDelete(SessionFactoryScope scope) {
		var bookType = scope.getSessionFactory().getJpaMetamodel().findEntityType(Book.class);
		assertNotNull(  bookType );
		@SuppressWarnings("unchecked")
		var title =
				(SingularAttribute<? super Book, String>)
						bookType.findSingularAttribute("title");
		@SuppressWarnings("unchecked")
		var isbn =
				(SingularAttribute<? super Book, String>)
						bookType.findSingularAttribute("isbn");

		scope.inTransaction( session -> {
			DeleteSpecification.create( Book.class )
					.restrict( Restriction.startsWith( title, "Hibernate" ) )
					.createQuery( session )
					.executeUpdate();
			var list =
					SimpleProjectionSpecification.create( SelectionSpecification.create( Book.class ), isbn )
							.createQuery( session )
							.getResultList();
			assertEquals( 1, list.size() );
			assertEquals( "9781617290459", list.get( 0 ) );
		} );

	}

	@Test void testCriteriaDelete(SessionFactoryScope scope) {
		var bookType = scope.getSessionFactory().getJpaMetamodel().findEntityType(Book.class);
		assertNotNull(  bookType );
		@SuppressWarnings("unchecked")
		var title =
				(SingularAttribute<? super Book, String>)
						bookType.findSingularAttribute("title");
		@SuppressWarnings("unchecked")
		var isbn =
				(SingularAttribute<? super Book, String>)
						bookType.findSingularAttribute("isbn");

		scope.inTransaction( session -> {
			var builder = session.getCriteriaBuilder();
			var query = builder.createCriteriaDelete( Book.class );
			var root = query.from( Book.class );
			DeleteSpecification.create( query )
					.restrict( Restriction.startsWith( title, "Hibernate" ) )
					.createQuery( session )
					.executeUpdate();
			var list =
					SimpleProjectionSpecification.create( SelectionSpecification.create( Book.class ), isbn )
							.createQuery( session )
							.getResultList();
			assertEquals( 1, list.size() );
			assertEquals( "9781617290459", list.get( 0 ) );
		} );

	}

	@Entity(name="Book")
	static class Book {
		@Id String isbn;
		String title;

		Book(String isbn, String title) {
			this.isbn = isbn;
			this.title = title;
		}

		Book() {
		}
	}
}
