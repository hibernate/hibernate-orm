/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.cascade;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Luis Barreiro
 */
@JiraKey( "HHH-10254" )
@DomainModel(
		annotatedClasses = {
			CascadeDetachedTest.Author.class, CascadeDetachedTest.Book.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class CascadeDetachedTest {

	@Test
	public void test(SessionFactoryScope scope) {
		Book book = new Book( "978-1118063330", "Operating System Concepts 9th Edition" );
		book.addAuthor( new Author( "Abraham", "Silberschatz", new char[] { 'a', 'b' } ) );
		book.addAuthor( new Author( "Peter", "Galvin", new char[] { 'c', 'd' }  ) );
		book.addAuthor( new Author( "Greg", "Gagne", new char[] { 'e', 'f' }  ) );

		scope.inTransaction( em -> {
					em.persist( book );
		} );

		scope.inTransaction( em -> {
			em.merge( book );
		} );
	}

	// --- //

	@Entity
	@Table( name = "BOOK" )
	public static class Book {

		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		Long id;

		String isbn;
		String title;

		@OneToMany( cascade = CascadeType.ALL, mappedBy = "book" )
		List<Author> authors = new ArrayList<>();

		public Book() {
		}

		public Book(String isbn, String title) {
			this.isbn = isbn;
			this.title = title;
		}

		public void addAuthor(Author author) {
			authors.add( author );
			author.book = this;
		}
	}

	@Entity
	@Table( name = "AUTHOR" )
	public static class Author {

		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		Long id;

		String firstName;
		String lastName;

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn
		Book book;

		@Basic( fetch = FetchType.LAZY )
		char[] charArrayCode;

		public Author() {
		}

		public Author(String firstName, String lastName, char[] charArrayCode) {
			this.firstName = firstName;
			this.lastName = lastName;
			this.charArrayCode = charArrayCode;
		}
	}
}
