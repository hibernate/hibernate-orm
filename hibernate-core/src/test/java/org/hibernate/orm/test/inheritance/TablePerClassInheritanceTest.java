package org.hibernate.orm.test.inheritance;

import java.util.Objects;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.inheritance.TablePerClassInheritanceTest.Author;
import static org.hibernate.orm.test.inheritance.TablePerClassInheritanceTest.Book;
import static org.hibernate.orm.test.inheritance.TablePerClassInheritanceTest.SpellBook;

@SessionFactory
@DomainModel(annotatedClasses = { Book.class, SpellBook.class, Author.class })
@TestForIssue(jiraKey = "HHH")
public class TablePerClassInheritanceTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final SpellBook spells = new SpellBook( 6, "Necronomicon", true );
					session.persist( spells );
				}
		);
	}

	@Test
	public void testUpdateAndDelete(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.createMutationQuery( "update SpellBook set forbidden=:fob where title=:tit" )
					.setParameter( "fob", false )
					.setParameter( "tit", "Necronomicon" )
					.executeUpdate();
		} );

		scope.inTransaction( s -> {
			s.createMutationQuery( "update Book set title=title||:sfx where title=:tit" )
					.setParameter( "sfx", " II" )
					.setParameter( "tit", "Necronomicon" )
					.executeUpdate();
		} );

		scope.inTransaction( s -> {
			Book book = s.find( Book.class, 6 );
			assertThat( book ).isInstanceOf( SpellBook.class );
			assertThat( book.getTitle() ).isEqualTo( "Necronomicon II" );
			assertThat( ( (SpellBook) book ).isForbidden() ).as( "Subclass field not updated" ).isFalse();
		} );

		scope.inTransaction( s -> {
			s.createMutationQuery( "delete Book where title='Necronomicon II'" ).executeUpdate();
		} );

		scope.inTransaction( s -> {
			Book book = s.find( Book.class, 6 );
			assertThat( book ).as( "Book not deleted - found " + book ).isNull();
		} );
	}

	@Entity(name = "SpellBook")
	@Table(name = "SpellBookUS")
	@DiscriminatorValue("S")
	public static class SpellBook extends Book {

		private boolean forbidden;

		public SpellBook(Integer id, String title, boolean forbidden) {
			super( id, title );
			this.forbidden = forbidden;
		}

		SpellBook() {
		}

		public boolean isForbidden() {
			return forbidden;
		}
	}

	@Entity(name = "Book")
	@Table(name = "BookUS")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
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

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Book book = (Book) o;
			return Objects.equals( title, book.title );
		}

		@Override
		public int hashCode() {
			return Objects.hash( title );
		}

		@Override
		public String toString() {
			return this.getClass().getSimpleName() + ":" + id + ":" + title;
		}
	}

	@Entity(name = "Author")
	@Table(name = "AuthorUS")
	public static class Author {

		@Id
		@GeneratedValue
		private Integer id;

		@Column(name = "`name`")
		private String name;

		@ManyToOne
		private Book book;

		public Author() {
		}

		public Author(String name, Book book) {
			this.name = name;
			this.book = book;
		}

		public Author(Integer id, String name, Book book) {
			this.id = id;
			this.name = name;
			this.book = book;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Book getBook() {
			return book;
		}

		public void setBook(Book book) {
			this.book = book;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Author author = (Author) o;
			return Objects.equals( name, author.name );
		}

		@Override
		public int hashCode() {
			return Objects.hash( name );
		}
	}

}
