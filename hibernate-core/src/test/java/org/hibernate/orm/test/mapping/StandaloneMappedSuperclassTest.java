package org.hibernate.orm.test.mapping;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedNativeStatement;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStatement;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(annotatedClasses = { StandaloneMappedSuperclassTest.Queries.class, StandaloneMappedSuperclassTest.Book.class })
class StandaloneMappedSuperclassTest {
	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-14742" )
	void namedQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em ->
				assertTrue( em.createNamedQuery( "allBooks", Book.class ).getResultList().isEmpty() ) );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-14917" )
	void resultSetMapping(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> em.createStoredProcedureQuery( "get_books", "bookIds" ) );
	}

	@Test
	void namedNativeQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em ->
				assertTrue( em.createNamedQuery( "allBooksNative", Book.class ).getResultList().isEmpty() ) );
	}

	@Test
	void namedStatement(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> assertEquals( 0, em.createNamedStatement( "deleteBooks" ).execute() ) );
	}

	@Test
	void namedNativeStatement(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> assertEquals( 0, em.createNamedStatement( "deleteBooksNative" ).execute() ) );
	}

	@Test
	void namedStoredProcedureQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> em.createNamedStoredProcedureQuery( "getBooks" ) );
	}

	@Test
	void fetchProfile(EntityManagerFactoryScope scope) {
		var factory = scope.getEntityManagerFactory().unwrap( SessionFactory.class );
		assertTrue( factory.containsFetchProfileDefinition( "bookProfile" ) );
	}

	@MappedSuperclass
	@FetchProfile(name = "bookProfile")
	@NamedQuery(name = "allBooks", query = "from Book")
	@NamedNativeQuery(name = "allBooksNative", query = "select id from books", resultClass = Book.class)
	@NamedStatement(name = "deleteBooks", statement = "delete from Book")
	@NamedNativeStatement(name = "deleteBooksNative", statement = "delete from books where 1=1")
	@NamedStoredProcedureQuery(name = "getBooks", procedureName = "get_books")
	@SqlResultSetMapping(name = "bookIds", classes = @ConstructorResult(
			targetClass = BookId.class, columns = @ColumnResult(name = "id", type = Long.class)
	))
	static class Queries {
	}

	record BookId(Long id) {}

	@Entity(name = "Book")
	@Table(name = "books")
	static class Book {
		@Id
		long id;
	}
}
