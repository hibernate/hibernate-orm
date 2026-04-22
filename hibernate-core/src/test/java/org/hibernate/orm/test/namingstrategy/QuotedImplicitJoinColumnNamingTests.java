/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BaseUnitTest
public class QuotedImplicitJoinColumnNamingTests {

	@Test
	@JiraKey("HHH-20042")
	public void testJpaCompliantDoesNotQuoteImplicitJoinColumnWhenReferencedTableIsQuoted() {
		withMetadata(
				ImplicitNamingStrategyJpaCompliantImpl.INSTANCE,
				metadata -> {
					final Column column = propertyColumn( metadata, Author.class, "book" );
					assertEquals( "book_isbn", column.getName() );
					assertFalse( column.isQuoted() );
				},
				Author.class,
				Book.class
		);
	}

	@Test
	@JiraKey("HHH-20042")
	public void testJpaCompliantStillQuotesImplicitJoinColumnWhenReferencedColumnIsQuoted() {
		withMetadata(
				ImplicitNamingStrategyJpaCompliantImpl.INSTANCE,
				metadata -> {
					final Column column = propertyColumn( metadata, AuthorWithQuotedIdBook.class, "book" );
					assertEquals( "book_isbn", column.getName() );
					assertTrue( column.isQuoted() );
				},
				AuthorWithQuotedIdBook.class,
				BookWithQuotedId.class
		);
	}

	@Test
	@JiraKey("HHH-20042")
	public void testLegacyJpaQuotesImplicitJoinColumnWhenQuotedTableNameIsUsed() {
		withMetadata(
				ImplicitNamingStrategyLegacyJpaImpl.INSTANCE,
				metadata -> {
					final Column column = collectionKeyColumn( metadata, Owner.class, "children" );
					assertEquals( "OwnerTable_id", column.getName() );
					assertTrue( column.isQuoted() );
				},
				Owner.class,
				Child.class
		);
	}

	private void withMetadata(
			ImplicitNamingStrategy implicitNamingStrategy,
			Consumer<MetadataImplementor> verifier,
			Class<?>... annotatedClasses) {
		final StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder().build();
		try {
			final MetadataSources metadataSources = new MetadataSources( serviceRegistry );
			for ( Class<?> annotatedClass : annotatedClasses ) {
				metadataSources.addAnnotatedClass( annotatedClass );
			}

			final MetadataImplementor metadata = (MetadataImplementor) metadataSources.getMetadataBuilder()
					.applyImplicitNamingStrategy( implicitNamingStrategy )
					.build();
			metadata.orderColumns( false );
			metadata.validate();

			verifier.accept( metadata );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	private Column propertyColumn(MetadataImplementor metadata, Class<?> entityClass, String propertyName) {
		final PersistentClass entityBinding = metadata.getEntityBinding( entityClass.getName() );
		final Property property = entityBinding.getProperty( propertyName );
		assertEquals( 1, property.getValue().getSelectables().size() );
		final Selectable selectable = property.getValue().getSelectables().get( 0 );
		assertInstanceOf( Column.class, selectable );
		return (Column) selectable;
	}

	private Column collectionKeyColumn(MetadataImplementor metadata, Class<?> entityClass, String propertyName) {
		final org.hibernate.mapping.Collection collection =
				metadata.getCollectionBinding( entityClass.getName() + '.' + propertyName );
		assertEquals( 1, collection.getKey().getColumns().size() );
		return collection.getKey().getColumns().get( 0 );
	}

	@Entity(name = "Author")
	public static class Author {
		@Id
		private String ssn;

		@ManyToOne
		private Book book;
	}

	@Entity(name = "Book")
	@Table(name = "`BookTable`")
	public static class Book {
		@Id
		private String isbn;
	}

	@Entity(name = "AuthorWithQuotedIdBook")
	public static class AuthorWithQuotedIdBook {
		@Id
		private String ssn;

		@ManyToOne
		private BookWithQuotedId book;
	}

	@Entity(name = "BookWithQuotedId")
	@Table(name = "`BookTable`")
	public static class BookWithQuotedId {
		@Id
		@jakarta.persistence.Column(name = "`isbn`")
		private String isbn;
	}

	@Entity(name = "Owner")
	@Table(name = "`OwnerTable`")
	public static class Owner {
		@Id
		private String id;

		@ManyToMany
		private Set<Child> children = new HashSet<>();
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		private String id;
	}
}
