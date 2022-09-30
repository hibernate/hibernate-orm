/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.embeddables;

import java.util.List;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				EmbeddableAndMappedSuperClassWithGenericsTest.PopularBook.class,
				EmbeddableAndMappedSuperClassWithGenericsTest.RareBook.class
		}
)
@SessionFactory
@TestForIssue(jiraKey = "HHH-15552")
public class EmbeddableAndMappedSuperClassWithGenericsTest {

	private final static long POPULAR_BOOK_ID = 1l;
	private final static long RARE_BOOK_ID = 2l;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Edition popularEdition = new Edition( "Popular" );
					PopularBook popularBook = new PopularBook( POPULAR_BOOK_ID, popularEdition, "POP" );

					Edition rareEdition = new Edition( "Rare" );
					RareBook rareBook = new RareBook( RARE_BOOK_ID, rareEdition, 123 );

					session.persist( popularBook );
					session.persist( rareBook );
				}
		);
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Integer> rareBookCodes = session.createQuery(
							"select b.code from RareBook b where b.id = :id",
							Integer.class
					).setParameter( "id", RARE_BOOK_ID ).list();

					assertThat( rareBookCodes.size() ).isEqualTo( 1 );

					Integer code = rareBookCodes.get( 0 );
					assertThat( code ).isEqualTo( 123 );
				}
		);

		scope.inTransaction(
				session -> {
					List<String> populareBookCodes = session.createQuery(
							"select b.code from PopularBook b where b.id = :id",
							String.class
					).setParameter( "id", POPULAR_BOOK_ID ).list();

					assertThat( populareBookCodes.size() ).isEqualTo( 1 );

					String code = populareBookCodes.get( 0 );
					assertThat( code ).isEqualTo( "POP" );
				}
		);
	}

	@Embeddable
	public static class Edition {
		private String name;

		public Edition() {
		}

		public Edition(String name) {
			this.name = name;
		}
	}

	@MappedSuperclass
	public static abstract class Book<T> {
		private Edition edition;

		@Column(name = "CODE_COLUMN")
		private T code;

		public Book() {
		}

		public Book(Edition edition, T code) {
			this.edition = edition;
			this.code = code;
		}
	}


	@Entity(name = "PopularBook")
	public static class PopularBook extends Book<String> {

		@Id
		private Long id;

		public PopularBook() {
		}

		public PopularBook(Long id, Edition edition, String code) {
			super( edition, code );
			this.id = id;
		}
	}

	@Entity(name = "RareBook")
	public static class RareBook extends Book<Integer> {

		@Id
		private Long id;

		public RareBook() {
		}

		public RareBook(Long id, Edition edition, Integer code) {
			super( edition, code );
			this.id = id;
		}

	}
}
