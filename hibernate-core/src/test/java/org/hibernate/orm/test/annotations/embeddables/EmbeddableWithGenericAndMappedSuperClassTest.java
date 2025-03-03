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

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				EmbeddableWithGenericAndMappedSuperClassTest.PopularBook.class,
				EmbeddableWithGenericAndMappedSuperClassTest.RareBook.class,
				EmbeddableWithGenericAndMappedSuperClassTest.GenericExample.class
		}
)
@SessionFactory
public class EmbeddableWithGenericAndMappedSuperClassTest {

	private final static long POPULAR_BOOK_ID = 1l;
	private final static String POPULAR_BOOK_CODE = "POP";
	private final static long RARE_BOOK_ID = 2l;
	private final static Integer RARE_BOOK_CODE = 123;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Edition popularEdition = new Edition<>( "Popular", POPULAR_BOOK_CODE );
					PopularBook popularBook = new PopularBook( POPULAR_BOOK_ID, popularEdition );

					Edition rareEdition = new Edition<>( "Rare", RARE_BOOK_CODE );
					RareBook rareBook = new RareBook( RARE_BOOK_ID, rareEdition );

					session.persist( popularBook );
					session.persist( rareBook );

					session.persist( new GenericExample(1, new Range<>(2, 3)) );
				}
		);
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from PopularBook" ).executeUpdate();
					session.createMutationQuery( "delete from RareBook" ).executeUpdate();
					session.createMutationQuery( "delete from GenericExample" ).executeUpdate();
				}
		);
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Integer> rareBookCodes = session.createQuery(
							"select b.edition.code from RareBook b where b.id = :id",
							Integer.class
					).setParameter( "id", RARE_BOOK_ID ).list();

					assertThat( rareBookCodes.size() ).isEqualTo( 1 );

					Integer code = rareBookCodes.get( 0 );
					assertThat( code ).isEqualTo( RARE_BOOK_CODE );
				}
		);

		scope.inTransaction(
				session -> {
					List<Integer> rareBookCodes = session.createQuery(
							"select c.code from Base b left join treat(b as RareBook).edition c where b.id = :id",
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
							"select b.edition.code from PopularBook b where b.id = :id",
							String.class
					).setParameter( "id", POPULAR_BOOK_ID ).list();

					assertThat( populareBookCodes.size() ).isEqualTo( 1 );

					String code = populareBookCodes.get( 0 );
					assertThat( code ).isEqualTo( POPULAR_BOOK_CODE );
				}
		);

		scope.inTransaction(
				session -> {
					List<String> rareBookCodes = session.createQuery(
							"select c.code from Base b left join treat(b as PopularBook).edition c where b.id = :id",
							String.class
					).setParameter( "id", POPULAR_BOOK_ID ).list();

					assertThat( rareBookCodes.size() ).isEqualTo( 1 );

					String code = rareBookCodes.get( 0 );
					assertThat( code ).isEqualTo( POPULAR_BOOK_CODE );
				}
		);
	}

	@Test
	public void testQueryParam(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Long> rareBookIds = session.createQuery(
							"select id from RareBook b where b.edition.code = :code",
							Long.class
					).setParameter( "code", RARE_BOOK_CODE ).list();

					assertThat( rareBookIds ).hasSize( 1 );

					Long id = rareBookIds.get( 0 );
					assertThat( id ).isEqualTo( RARE_BOOK_ID );
				}
		);

		scope.inTransaction(
				session -> {
					List<Long> rareBookIds = session.createQuery(
							"select id from RareBook b where b.edition = :edition",
							Long.class
					).setParameter( "edition", new Edition<>( "Rare", RARE_BOOK_CODE ) ).list();

					assertThat( rareBookIds ).hasSize( 1 );

					Long id = rareBookIds.get( 0 );
					assertThat( id ).isEqualTo( RARE_BOOK_ID );
				}
		);

		scope.inTransaction(
				session -> {
					List<Long> popularBookIds = session.createQuery(
							"select id from PopularBook b where b.edition.code = :code",
							Long.class
					).setParameter( "code", POPULAR_BOOK_CODE ).list();

					assertThat( popularBookIds ).hasSize( 1 );

					Long id = popularBookIds.get( 0 );
					assertThat( id ).isEqualTo( POPULAR_BOOK_ID );
				}
		);

		scope.inTransaction(
				session -> {
					List<Long> popularBookIds = session.createQuery(
							"select id from PopularBook b where b.edition = :edition",
							Long.class
					).setParameter( "edition", new Edition<>( "Popular", POPULAR_BOOK_CODE ) ).list();

					assertThat( popularBookIds ).hasSize( 1 );

					Long id = popularBookIds.get( 0 );
					assertThat( id ).isEqualTo( POPULAR_BOOK_ID );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-4299")
	public void testGenericEmbeddedAttribute(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					GenericExample ge = session.get( GenericExample.class, 1 );
					assertThat( ge ).isNotNull();
					assertThat( ge ).extracting( "bounds" ).isNotNull();
				}
		);
	}

	@Entity(name = "GenericExample")
	public static class GenericExample {
		@Id
		private int id;
		@Embedded
		private Range<Integer> bounds;

		public GenericExample() {
		}

		public GenericExample(int id, Range<Integer> bounds) {
			this.id = id;
			this.bounds = bounds;
		}
	}

	public static class Range<T> {

		private T minimum;
		private T maximum;

		public Range() {
		}

		public Range(T minimum, T maximum) {
			this.minimum = minimum;
			this.maximum = maximum;
		}
	}

	@Embeddable
	public static class Edition<T> {
		private String editorName;

		@Column(name = "CODE_COLUMN")
		private T code;

		public Edition() {
		}

		public Edition(String editorName, T code) {
			this.editorName = editorName;
			this.code = code;
		}
	}

	@Entity(name = "Base")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@DiscriminatorColumn(name = "PROP_TYPE")
	public abstract static class Base {
		@Id
		private Long id;


		public Base() {
		}

		public Base(Long id) {
			this.id = id;
		}
	}

	@MappedSuperclass
	public static abstract class Book<T> extends Base {
		private Edition<T> edition;

		public Book() {
		}

		public Book(Long id, Edition<T> edition) {
			super( id );
			this.edition = edition;
		}
	}


	@Entity(name = "PopularBook")
	@AttributeOverride( name = "edition.code", column = @Column(name = "code_str"))
	public static class PopularBook extends Book<String> {


		public PopularBook() {
		}

		public PopularBook(Long id, Edition<String> edition) {
			super( id, edition );
		}
	}

	@Entity(name = "RareBook")
	@AttributeOverride( name = "edition.code", column = @Column(name = "code_nr"))
	public static class RareBook extends Book<Integer> {

		public RareBook() {
		}

		public RareBook(Long id, Edition<Integer> edition) {
			super( id, edition );
		}

	}
}
