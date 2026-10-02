package org.hibernate.orm.test.jpa.nameddescriptor;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Exercises package-level named queries and statements, matching the JPA 4 TCK scenario.
///
/// @author Steve Ebersole
@Jpa(
		annotatedClasses = PackageDescriptorNamedQueriesTest.Book.class,
		annotatedPackageNames = "org.hibernate.orm.test.jpa.nameddescriptor",
		excludeUnlistedClasses = true
)
public class PackageDescriptorNamedQueriesTest {
	@Test
	void packageDescriptorNamedQueryAndStatementExecution(EntityManagerFactoryScope scope) {
		verifyNamedQueriesAndStatement( scope.getEntityManagerFactory() );
	}

	static void verifyNamedQueriesAndStatement(EntityManagerFactory factory) {
		factory.runInTransaction( entityManager -> entityManager.persist( new Book( 1, "Alpha" ) ) );

		factory.runInTransaction( entityManager -> {
			final Book byJpql = entityManager.createNamedQuery( "DescriptorBook.byTitle", Book.class )
					.setParameter( "title", "Alpha" )
					.getSingleResult();
			assertThat( byJpql.id ).isEqualTo( 1 );

			final Book byNative = entityManager.createNamedQuery( "DescriptorBook.byTitleNative", Book.class )
					.setParameter( 1, "Alpha" )
					.getSingleResult();
			assertThat( byNative.id ).isEqualTo( 1 );

			final int updated = entityManager.createNamedStatement( "DescriptorBook.rename" )
					.setParameter( "title", "Renamed" )
					.setParameter( "id", 1 )
					.execute();
			assertThat( updated ).isEqualTo( 1 );
			entityManager.clear();
			assertThat( entityManager.find( Book.class, 1 ).title ).isEqualTo( "Renamed" );
		} );
	}

	@AfterEach
	void cleanUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> entityManager.createQuery( "delete from DescriptorBook" ).executeUpdate() );
	}

	@Entity(name = "DescriptorBook")
	@Table(name = "descriptor_book")
	public static class Book {
		@Id
		private Integer id;
		private String title;

		public Book() {
		}

		public Book(Integer id, String title) {
			this.id = id;
			this.title = title;
		}
	}
}
