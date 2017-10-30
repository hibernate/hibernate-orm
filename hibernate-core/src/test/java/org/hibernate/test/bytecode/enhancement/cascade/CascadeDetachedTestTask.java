/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.cascade;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

/**
 * @author Luis Barreiro
 */

@TestForIssue(jiraKey = "HHH-10254")
public class CascadeDetachedTestTask extends AbstractEnhancerTestTask {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Author.class, Book.class };
	}

	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		super.prepare( cfg );
	}

	public void execute() {
		Book book = new Book( "978-1118063330", "Operating System Concepts 9th Edition" );
		book.addAuthor( new Author( "Abraham", "Silberschatz", new char[] { 'a', 'b' } ) );
		book.addAuthor( new Author( "Peter", "Galvin", new char[] { 'c', 'd' } ) );
		book.addAuthor( new Author( "Greg", "Gagne", new char[] { 'e', 'f' } ) );

		Session s = getFactory().openSession();
		s.beginTransaction();
		s.persist( book );
		s.getTransaction().commit();
		s.close();

		s = getFactory().openSession();
		s.beginTransaction();
		s.merge( book );
		s.getTransaction().commit();
		s.close();
	}

	protected void cleanup() {
	}

	// --- //

	@Entity
	@Table(name = "BOOK")
	public static class Book {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		String isbn;
		String title;

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "book")
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
	@Table(name = "AUTHOR")
	public static class Author {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		String firstName;
		String lastName;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn
		Book book;

		@Basic(fetch = FetchType.LAZY)
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
