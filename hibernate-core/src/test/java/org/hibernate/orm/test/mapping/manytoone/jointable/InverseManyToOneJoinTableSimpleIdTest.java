/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.manytoone.jointable;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.ManyToManyCollectionPart;
import org.hibernate.metamodel.mapping.internal.SimpleForeignKeyDescriptor;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts the runtime model descriptors related to the inverse side
 * of a many-to-one with join-table.
 * <p/>
 * This tests simple keys.  See {@link InverseManyToOneJoinTableCompositeIdTest} for
 * composite id testing
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = { InverseManyToOneJoinTableSimpleIdTest.Book.class, InverseManyToOneJoinTableSimpleIdTest.Author.class } )
@SessionFactory
public class InverseManyToOneJoinTableSimpleIdTest {
	@Test
	public void assertModel(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();

		final EntityPersister entityDescriptor = mappingMetamodel.getEntityDescriptor( Author.class );
		final PluralAttributeMapping books = (PluralAttributeMapping) entityDescriptor.findAttributeMapping( "books" );
		final ManyToManyCollectionPart booksElementDescriptor = (ManyToManyCollectionPart) books.getElementDescriptor();
		final SimpleForeignKeyDescriptor booksFk = (SimpleForeignKeyDescriptor) booksElementDescriptor.getForeignKeyDescriptor();

		assertThat( booksFk.getKeyTable() ).isEqualTo( "book_authors" );
		assertThat( booksFk.getKeyPart().getSelectionExpression() ).isEqualTo( "book_id" );

		assertThat( booksFk.getTargetTable() ).isEqualTo( "books" );
		assertThat( booksFk.getTargetPart().getSelectionExpression() ).isEqualTo( "id" );
	}

	@Test
	public void usageSmokeTest(SessionFactoryScope scope) {
		createTestData( scope );

		try {
			scope.inTransaction( (session) -> {
				final Author stephenKing = session.find( Author.class, 1 );
				verifyStephenKingBooks( stephenKing );
			} );

			scope.inTransaction( (session) -> {
				final Author stephenKing = session
						.createSelectionQuery( "from Author a join fetch a.books where a.id = 1", Author.class )
						.getSingleResult();
				verifyStephenKingBooks( stephenKing );
			} );
		}
		finally {
			dropTestData( scope );
		}
	}

	private void verifyStephenKingBooks(Author author) {
		final List<String> bookNames = author.books.stream().map( Book::getName ).collect( Collectors.toList() );
		assertThat( bookNames ).contains( "It", "The Shining" );
	}

	private void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Author stephenKing = new Author( 1, "Stephen King" );
			final Author johnMilton = new Author( 2, "John Milton" );
			session.persist( stephenKing );
			session.persist( johnMilton );

			session.persist( new Book( 1, "It", stephenKing ) );
			session.persist( new Book( 2, "The Shining", stephenKing ) );

			session.persist( new Book( 3, "Paradise Lost", johnMilton ) );
		} );
	}

	private void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "from Author", Author.class ).list().forEach( session::remove );
		} );
		scope.inTransaction( (session) -> {
			final Long bookCount = session.createSelectionQuery( "select count(1) from Book", Long.class ).uniqueResult();
			assertThat( bookCount ).isEqualTo( 0L );
		} );
	}

	@Entity( name = "Book" )
	@Table( name = "books" )
	public static class Book {
		@Id
		private Integer id;
		@Basic
		private String name;
		@ManyToOne
		@JoinTable(name = "book_authors",
				joinColumns = @JoinColumn(name = "book_id"),
				inverseJoinColumns = @JoinColumn(name="author_id",nullable = false))
		private Author author;

		private Book() {
			// for use by Hibernate
		}

		public Book(Integer id, String name, Author author) {
			this.id = id;
			this.name = name;
			this.author = author;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "Author" )
	@Table( name = "authors" )
	public static class Author {
		@Id
		private Integer id;
		@Basic
		private String name;

		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "author")
		private List<Book> books;

		private Author() {
			// for use by Hibernate
		}

		public Author(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
