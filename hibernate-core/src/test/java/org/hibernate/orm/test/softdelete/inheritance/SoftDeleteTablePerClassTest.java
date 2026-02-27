/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.inheritance;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DomainModel(annotatedClasses = {
		SoftDeleteTablePerClassTest.Book.class,
		SoftDeleteTablePerClassTest.SpellBook.class,
		SoftDeleteTablePerClassTest.DarkSpellBook.class,
		SoftDeleteTablePerClassTest.Novel.class
})
@SessionFactory
public class SoftDeleteTablePerClassTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new SpellBook( 1, "Necronomicon", true ) );
			session.persist( new SpellBook( 2, "Book of Shadows", false ) );
			session.persist( new Novel( 3, "The Hobbit", "Fantasy" ) );
			session.persist( new DarkSpellBook( 4, "Book of Vile Darkness", true, "Necromancy" ) );
			session.persist( new DarkSpellBook( 5, "Necronomicon Ex-Mortis", true, "Dark Arts" ) );
			session.persist( new Novel( 6, "1984", "Dystopian" ) );
			session.persist( new DarkSpellBook( 7, "Grimoire of Shadows", true, "Illusion" ) );
		} );
		// Soft-delete via HQL bulk delete
		scope.inTransaction( session -> {
			// direct subclass delete
			session.createMutationQuery( "delete from SpellBook where id = :id" )
					.setParameter( "id", 1 )
					.executeUpdate();
			// root-level polymorphic delete targeting a nested subclass
			session.createMutationQuery( "delete from Book where id = :id" )
					.setParameter( "id", 5 )
					.executeUpdate();
			// mid-level polymorphic delete: SpellBook query targeting a DarkSpellBook row
			session.createMutationQuery( "delete from SpellBook where id = :id" )
					.setParameter( "id", 7 )
					.executeUpdate();
		} );
		// Soft-delete via session.remove()
		scope.inTransaction( session -> {
			final var novel = session.find( Novel.class, 6 );
			session.remove( novel );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	void testDeletedFlagsAreSetCorrectly(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// SpellBook(1) soft-deleted via HQL, SpellBook(2) active
			assertThat( session.createNativeQuery(
					"select title, deleted from " + SpellBook.TABLE_NAME + " order by id", Tuple.class
			).getResultList() )
					.extracting( t -> t.get( "title" ), t -> t.get( "deleted" ) )
					.containsExactly( tuple( "Necronomicon", true ), tuple( "Book of Shadows", false ) );

			// DarkSpellBook(5) deleted via root-level "delete from Book",
			// DarkSpellBook(7) deleted via mid-level "delete from SpellBook",
			// DarkSpellBook(4) active
			assertThat( session.createNativeQuery(
					"select title, deleted from " + DarkSpellBook.TABLE_NAME + " order by id", Tuple.class
			).getResultList() )
					.extracting( t -> t.get( "title" ), t -> t.get( "deleted" ) )
					.containsExactly(
							tuple( "Book of Vile Darkness", false ),
							tuple( "Necronomicon Ex-Mortis", true ),
							tuple( "Grimoire of Shadows", true )
					);

			// Novel(6) soft-deleted via session.remove(), Novel(3) active
			assertThat( session.createNativeQuery(
					"select title, deleted from " + Novel.TABLE_NAME + " order by id", Tuple.class
			).getResultList() )
					.extracting( t -> t.get( "title" ), t -> t.get( "deleted" ) )
					.containsExactly( tuple( "The Hobbit", false ), tuple( "1984", true ) );
		} );
	}

	@Test
	void testQueriesExcludeSoftDeletedEntities(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// find() returns null for all soft-deleted entities
			assertThat( session.find( SpellBook.class, 1 ) ).isNull();
			assertThat( session.find( DarkSpellBook.class, 5 ) ).isNull();
			assertThat( session.find( Book.class, 6 ) ).isNull();
			assertThat( session.find( DarkSpellBook.class, 7 ) ).isNull();

			// Polymorphic root query
			assertThat( session.createSelectionQuery( "from Book", Book.class ).getResultList() )
					.extracting( Book::getTitle )
					.containsExactlyInAnyOrder( "Book of Shadows", "The Hobbit", "Book of Vile Darkness" );

			// Mid-level subclass query
			assertThat( session.createSelectionQuery( "from SpellBook", SpellBook.class ).getResultList() )
					.extracting( Book::getTitle )
					.containsExactlyInAnyOrder( "Book of Shadows", "Book of Vile Darkness" );

			// Leaf subclass query
			assertThat( session.createSelectionQuery( "from DarkSpellBook", DarkSpellBook.class ).getResultList() )
					.extracting( Book::getTitle )
					.containsExactly( "Book of Vile Darkness" );

			// Sibling subclass query
			assertThat( session.createSelectionQuery( "from Novel", Novel.class ).getResultList() )
					.extracting( Novel::getGenre )
					.containsExactly( "Fantasy" );
		} );
	}

	@Entity(name = "Book")
	@Table(name = Book.TABLE_NAME)
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@SoftDelete
	public static class Book {
		private static final String TABLE_NAME = "book_table";

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
	}

	@Entity(name = "SpellBook")
	@Table(name = SpellBook.TABLE_NAME)
	public static class SpellBook extends Book {
		private static final String TABLE_NAME = "spell_book_table";

		private boolean forbidden;

		public SpellBook() {
		}

		public SpellBook(Integer id, String title, boolean forbidden) {
			super( id, title );
			this.forbidden = forbidden;
		}

		public boolean isForbidden() {
			return forbidden;
		}

		public void setForbidden(boolean forbidden) {
			this.forbidden = forbidden;
		}
	}

	@Entity(name = "DarkSpellBook")
	@Table(name = DarkSpellBook.TABLE_NAME)
	public static class DarkSpellBook extends SpellBook {
		private static final String TABLE_NAME = "dark_spell_book_table";

		private String school;

		public DarkSpellBook() {
		}

		public DarkSpellBook(Integer id, String title, boolean forbidden, String school) {
			super( id, title, forbidden );
			this.school = school;
		}

		public String getSchool() {
			return school;
		}

		public void setSchool(String school) {
			this.school = school;
		}
	}

	@Entity(name = "Novel")
	@Table(name = Novel.TABLE_NAME)
	public static class Novel extends Book {
		private static final String TABLE_NAME = "novel_table";

		private String genre;

		public Novel() {
		}

		public Novel(Integer id, String title, String genre) {
			super( id, title );
			this.genre = genre;
		}

		public String getGenre() {
			return genre;
		}

		public void setGenre(String genre) {
			this.genre = genre;
		}
	}
}
