/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import org.checkerframework.checker.nullness.qual.NonNull;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		EntityInterfaceExtendsMapTest.BookEntity.class,
		EntityInterfaceExtendsMapTest.LibraryEntity.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17827" )
public class EntityInterfaceExtendsMapTest {
	@Test
	public void testMappingWorks(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final LibraryEntity library = session.find( LibraryEntity.class, 1L );
			assertThat( library.containsKey( "Dune Messiah" ) ).isTrue();
			assertThat( library.keySet() ).containsExactlyInAnyOrder( "Dune Messiah", "Clean Code" );
			assertThat( library.values() ).containsAll( library.books );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final BookEntity book1 = new BookEntity( "Dune Messiah", "Frank Herbert" );
			final BookEntity book2 = new BookEntity( "Clean Code", "Robert C. Martin" );
			final LibraryEntity library = new LibraryEntity();
			library.addBook( book1 );
			library.addBook( book2 );
			session.persist( library );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from LibraryEntity" ).executeUpdate();
			session.createMutationQuery( "delete from BookEntity" ).executeUpdate();
		} );
	}

	@Entity( name = "BookEntity" )
	static class BookEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String title;

		private String author;

		public BookEntity() {
		}

		public BookEntity(String title, String author) {
			this.title = title;
			this.author = author;
		}

		public String getTitle() {
			return title;
		}

		public String getAuthor() {
			return author;
		}
	}

	interface Library extends Map<String, BookEntity> {
	}

	@Entity( name = "LibraryEntity" )
	static class LibraryEntity implements Library {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToMany( cascade = CascadeType.ALL )
		private Set<BookEntity> books = new HashSet<>();

		private void addBook(BookEntity book) {
			put( book.getTitle(), book );
		}

		@Override
		public int size() {
			return books.size();
		}

		@Override
		public boolean isEmpty() {
			return books.isEmpty();
		}

		@Override
		public boolean containsKey(Object key) {
			return books.stream().anyMatch( b -> b.getTitle().equals( key ) );
		}

		@Override
		public boolean containsValue(Object value) {
			return books.stream().anyMatch( b -> b.equals( value ) );
		}

		@Override
		public BookEntity get(Object key) {
			for ( final BookEntity book : books ) {
				if ( book.getTitle().equals( key ) ) {
					return book;
				}
			}
			return null;
		}

		@Override
		public BookEntity put(String key, BookEntity value) {
			assert value.getTitle().equals( key );
			final BookEntity old = get( key );
			if ( books.add( value ) && old != null ) {
				throw new AssertionError();
			}
			return old;
		}

		@Override
		public BookEntity remove(Object key) {
			final Iterator<BookEntity> iterator = books.iterator();
			while ( iterator.hasNext() ) {
				final BookEntity next = iterator.next();
				if ( next.getTitle().equals( key ) ) {
					iterator.remove();
					return next;
				}
			}
			return null;
		}

		@Override
		public void putAll(Map<? extends String, ? extends BookEntity> m) {
			books.addAll( m.values() );
		}

		@Override
		public void clear() {
			books.clear();
		}

		@Override
		@NonNull
		public Set<String> keySet() {
			return books.stream().map( BookEntity::getTitle ).collect( Collectors.toUnmodifiableSet() );
		}

		@Override
		@NonNull
		public Collection<BookEntity> values() {
			return Collections.unmodifiableSet( books );
		}

		@Override
		@NonNull
		public Set<Entry<String, BookEntity>> entrySet() {
			return books.stream()
					.map( b -> new AbstractMap.SimpleEntry<>( b.getTitle(), b ) )
					.collect( Collectors.toUnmodifiableSet() );
		}
	}
}
