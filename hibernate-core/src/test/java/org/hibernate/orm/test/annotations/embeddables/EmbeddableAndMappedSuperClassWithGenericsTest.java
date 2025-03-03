/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embeddables;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
@JiraKey(value = "HHH-15552")
public class EmbeddableAndMappedSuperClassWithGenericsTest {

	private final static long POPULAR_BOOK_ID = 1l;
	private final static String POPULAR_BOOK_CODE = "POP";
	private final static long RARE_BOOK_ID = 2l;
	private final static Integer RARE_BOOK_CODE = 123;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Edition popularEdition = new Edition( "Popular" );
					PopularBook popularBook = new PopularBook( POPULAR_BOOK_ID, popularEdition, POPULAR_BOOK_CODE );

					Edition rareEdition = new Edition( "Rare" );
					RareBook rareBook = new RareBook( RARE_BOOK_ID, rareEdition, RARE_BOOK_CODE );

					session.persist( popularBook );
					session.persist( rareBook );
				}
		);
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from PopularBook" ).executeUpdate();
			session.createMutationQuery( "delete from RareBook" ).executeUpdate();
		} );
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
					assertThat( code ).isEqualTo( RARE_BOOK_CODE );
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
					assertThat( code ).isEqualTo( POPULAR_BOOK_CODE );
				}
		);
	}

	@Test
	public void testQueryParam(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Long> rareBookIds = session.createQuery(
							"select id from RareBook b where b.code = :code",
							Long.class
					).setParameter( "code", RARE_BOOK_CODE ).list();

					assertThat( rareBookIds ).hasSize( 1 );

					Long id = rareBookIds.get( 0 );
					assertThat( id ).isEqualTo( RARE_BOOK_ID );
				}
		);

		scope.inTransaction(
				session -> {
					List<Long> populareBookIds = session.createQuery(
							"select id from PopularBook b where b.code = :code",
							Long.class
					).setParameter( "code", POPULAR_BOOK_CODE ).list();

					assertThat( populareBookIds ).hasSize( 1 );

					Long id = populareBookIds.get( 0 );
					assertThat( id ).isEqualTo( POPULAR_BOOK_ID );
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
