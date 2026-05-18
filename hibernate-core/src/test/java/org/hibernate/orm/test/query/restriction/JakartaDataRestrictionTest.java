/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.restriction;

import jakarta.data.constraint.In;
import jakarta.data.constraint.Like;
import jakarta.data.expression.TemporalExpression;
import jakarta.data.metamodel.NavigableAttribute;
import jakarta.data.metamodel.NumericAttribute;
import jakarta.data.metamodel.TemporalAttribute;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.restrict.Restrict;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.query.restriction.JakartaDataRestriction;
import org.hibernate.query.specification.SelectionSpecification;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hibernate.query.Order.asc;
import static org.hibernate.query.restriction.JakartaDataRestriction.from;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SessionFactory
@DomainModel(annotatedClasses = {
		JakartaDataRestrictionTest.Book.class,
		JakartaDataRestrictionTest.Publisher.class
})
public class JakartaDataRestrictionTest {
	private static final TextAttribute<Book> ISBN = TextAttribute.of( Book.class, "isbn" );
	private static final TextAttribute<Book> TITLE = TextAttribute.of( Book.class, "title" );
	private static final NumericAttribute<Book, Integer> PAGES =
			NumericAttribute.of( Book.class, "pages", int.class );
	private static final TextAttribute<Book> SUBTITLE = TextAttribute.of( Book.class, "subtitle" );
	private static final TemporalAttribute<Book, LocalDate> PUBLISHED_ON =
			TemporalAttribute.of( Book.class, "publishedOn", LocalDate.class );
	private static final TemporalAttribute<Book, LocalDateTime> INDEXED_AT =
			TemporalAttribute.of( Book.class, "indexedAt", LocalDateTime.class );
	private static final NavigableAttribute<Book, Publisher> PUBLISHER =
			NavigableAttribute.of( Book.class, "publisher", Publisher.class );
	private static final TextAttribute<Publisher> NAME = TextAttribute.of( Publisher.class, "name" );

	@Test
	void basicAttributeRestrictions(SessionFactoryScope scope) {
		persistBooks( scope );

		assertTitles( scope, TITLE.contains( "Hibernate" ),
				"Hibernate in Action",
				"Java Persistence with Hibernate" );
		assertTitles( scope, PAGES.greaterThan( 500 ),
				"Java Persistence with Hibernate" );
		assertTitles( scope, PAGES.between( 200, 450 ),
				"Hibernate in Action",
				"Jakarta Data Guide" );
		assertTitles( scope, PAGES.notBetween( 300, 900 ),
				"Jakarta Data Guide",
				"Java Persistence with Hibernate" );
		assertTitles( scope, PAGES.lessThanEqual( 400 ),
				"Hibernate in Action",
				"Jakarta Data Guide" );
		assertTitles( scope, ISBN.in( List.of( "9781932394153", "9780131889988" ) ),
				"Hibernate in Action",
				"Jakarta Data Guide" );
		assertTitles( scope, ISBN.notIn( List.of( "9781932394153", "9780131889988" ) ),
				"Java Persistence with Hibernate" );
		assertTitles( scope, PAGES.in( List.of( 250, 400 ) ),
				"Hibernate in Action",
				"Jakarta Data Guide" );
		assertTitles( scope, PAGES.satisfies( In.expressions( List.of( PAGES.plus( 0 ) ) ) ),
				"Hibernate in Action",
				"Jakarta Data Guide",
				"Java Persistence with Hibernate" );
		assertTitles( scope, TITLE.notContains( "Hibernate" ),
				"Jakarta Data Guide" );
		assertTitles( scope, SUBTITLE.isNull(),
				"Jakarta Data Guide",
				"Java Persistence with Hibernate" );
		assertTitles( scope, SUBTITLE.notNull(),
				"Hibernate in Action" );
	}

	@Test
	void compositeAndNegatedRestrictions(SessionFactoryScope scope) {
		persistBooks( scope );

		assertTitles( scope, Restrict.all( TITLE.contains( "Hibernate" ), PAGES.lessThan( 500 ) ),
				"Hibernate in Action" );
		assertTitles( scope, Restrict.any( TITLE.startsWith( "Jakarta" ), PAGES.greaterThan( 900 ) ),
				"Jakarta Data Guide",
				"Java Persistence with Hibernate" );
		assertTitles( scope, Restrict.not( TITLE.contains( "Hibernate" ) ),
				"Jakarta Data Guide" );
		assertTitles( scope, Restrict.unrestricted(),
				"Hibernate in Action",
				"Jakarta Data Guide",
				"Java Persistence with Hibernate" );
		assertTitles( scope, Restrict.not( Restrict.unrestricted() ) );
		assertTitles( scope, JakartaDataRestriction.all(
						TITLE.contains( "Hibernate" ),
						PAGES.lessThanEqual( 400 )
				),
				"Hibernate in Action" );
		final List<jakarta.data.restrict.Restriction<? super Book>> restrictions =
				List.of( TITLE.contains( "Hibernate" ), PAGES.lessThanEqual( 400 ) );
		assertTitles( scope, JakartaDataRestriction.all(
						restrictions
				),
				"Hibernate in Action" );
	}

	@Test
	void navigablePathRestrictions(SessionFactoryScope scope) {
		persistBooks( scope );

		assertTitles( scope, PUBLISHER.navigate( NAME ).equalTo( "Manning" ),
				"Hibernate in Action",
				"Java Persistence with Hibernate" );
		assertTitles( scope, PUBLISHER.navigate( NAME ).notEqualTo( "Manning" ),
				"Jakarta Data Guide" );
	}

	@Test
	void expressionFunctionRestrictions(SessionFactoryScope scope) {
		persistBooks( scope );

		assertTitles( scope, TITLE.lower().contains( "hibernate" ),
				"Hibernate in Action",
				"Java Persistence with Hibernate" );
		assertTitles( scope, TITLE.upper().contains( "HIBERNATE" ),
				"Hibernate in Action",
				"Java Persistence with Hibernate" );
		assertTitles( scope, TITLE.length().greaterThan( 20 ),
				"Java Persistence with Hibernate" );
		assertTitles( scope, PAGES.plus( 50 ).lessThan( 500 ),
				"Hibernate in Action",
				"Jakarta Data Guide" );
		assertTitles( scope, PAGES.minus( 300 ).abs().lessThanEqual( 100 ),
				"Hibernate in Action",
				"Jakarta Data Guide" );
		assertTitles( scope, PAGES.negated().lessThan( -500 ),
				"Java Persistence with Hibernate" );
		assertTitles( scope, PAGES.times( 2 ).greaterThan( 1000 ),
				"Java Persistence with Hibernate" );
		assertTitles( scope, PAGES.dividedBy( 2 ).greaterThan( 300 ),
				"Java Persistence with Hibernate" );
		assertTitles( scope, PAGES.dividedInto( 1000 ).greaterThan( 2 ),
				"Jakarta Data Guide" );
		assertTitles( scope, PAGES.subtractedFrom( 1000 ).lessThan( 100 ),
				"Java Persistence with Hibernate" );
		assertTitles( scope, TITLE.left( 7 ).equalTo( "Jakarta" ),
				"Jakarta Data Guide" );
		assertTitles( scope, TITLE.right( 9 ).equalTo( "Hibernate" ),
				"Java Persistence with Hibernate" );
		assertTitles( scope, TITLE.prepend( "Guide: " ).startsWith( "Guide: Jakarta" ),
				"Jakarta Data Guide" );
		assertTitles( scope, TITLE.append( " Guide" ).equalTo( "Jakarta Data Guide Guide" ),
				"Jakarta Data Guide" );
		assertTitles( scope, TITLE.like( Like.literal( "Hibernate in Action" ) ),
				"Hibernate in Action" );
		assertTitles( scope, TITLE.like( Like.pattern( "Java*", '?', '*' ) ),
				"Java Persistence with Hibernate" );
		assertTitles( scope, TITLE.notLike( "Jakarta Data Guide" ),
				"Hibernate in Action",
				"Java Persistence with Hibernate" );
		assertTitles( scope, TITLE.endsWith( "Guide" ),
				"Jakarta Data Guide" );
		assertTitles( scope, TITLE.notStartsWith( "Hibernate" ),
				"Jakarta Data Guide",
				"Java Persistence with Hibernate" );
		assertTitles( scope, TITLE.notEndsWith( "Hibernate" ),
				"Hibernate in Action",
				"Jakarta Data Guide" );
	}

	@Test
	void currentTemporalExpressionRestrictions(SessionFactoryScope scope) {
		persistBooks( scope );

		assertTitles( scope, Restrict.all(
						TemporalExpression.localDate().notNull(),
						TemporalExpression.localTime().notNull(),
						TemporalExpression.localDateTime().notNull()
				),
				"Hibernate in Action",
				"Jakarta Data Guide",
				"Java Persistence with Hibernate" );
		assertTitles( scope, PUBLISHED_ON.lessThanEqual( TemporalExpression.localDate() ),
				"Hibernate in Action",
				"Java Persistence with Hibernate" );
		assertTitles( scope, INDEXED_AT.lessThanEqual( TemporalExpression.localDateTime() ),
				"Hibernate in Action",
				"Jakarta Data Guide",
				"Java Persistence with Hibernate" );
	}

	@Test
	void numericCastRestrictions(SessionFactoryScope scope) {
		persistBooks( scope );

		assertTitles( scope, PAGES.asLong().greaterThan( 500L ),
				"Java Persistence with Hibernate" );
		assertTitles( scope, PAGES.asDouble().between( 200.0, 450.0 ),
				"Hibernate in Action",
				"Jakarta Data Guide" );
		assertTitles( scope, PAGES.asBigInteger().greaterThanEqual( BigInteger.valueOf( 1000 ) ),
				"Java Persistence with Hibernate" );
		assertTitles( scope, PAGES.asBigDecimal().lessThan( BigDecimal.valueOf( 500 ) ),
				"Hibernate in Action",
				"Jakarta Data Guide" );
	}

	@Test
	void mismatchedInRestrictionTypesAreRejected(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Book.class );
			final var root = query.from( Book.class );

			final var mismatchedValue = assertThrows(
					UnsupportedOperationException.class,
					() -> JakartaDataRestriction.predicate(
							root.get( "pages" ),
							In.values( "not a page count" ),
							root,
							builder
					)
			);
			assertEquals(
					"Expected java.lang.Integer value but got: java.lang.String",
					mismatchedValue.getMessage()
			);

			final var mismatchedExpression = assertThrows(
					UnsupportedOperationException.class,
					() -> JakartaDataRestriction.predicate(
							root.get( "pages" ),
							In.expressions( TITLE ),
							root,
							builder
					)
			);
			assertEquals(
					"Expected java.lang.Integer expression but got: java.lang.String",
					mismatchedExpression.getMessage()
			);
		} );
	}

	private static void assertTitles(
			SessionFactoryScope scope,
			jakarta.data.restrict.Restriction<? super Book> restriction,
			String... expectedTitles) {
		final List<String> titles = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( from( restriction ) )
						.sort( asc( Book.class, TITLE.name() ) )
						.createQuery( session )
						.getResultList()
						.stream()
						.map( book -> book.title )
						.toList() );
		assertEquals( List.of( expectedTitles ), titles );
	}

	private static void assertTitles(
			SessionFactoryScope scope,
			org.hibernate.query.restriction.Restriction<Book> restriction,
			String... expectedTitles) {
		final List<String> titles = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( restriction )
						.sort( asc( Book.class, TITLE.name() ) )
						.createQuery( session )
						.getResultList()
						.stream()
						.map( book -> book.title )
						.toList() );
		assertEquals( List.of( expectedTitles ), titles );
	}

	private static void persistBooks(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inTransaction( session -> {
			final Publisher manning = new Publisher( 1, "Manning" );
			final Publisher eclipse = new Publisher( 2, "Eclipse Foundation" );
			session.persist( manning );
			session.persist( eclipse );
			session.persist( new Book( "9781932394153", "Hibernate in Action", "Classic", 400,
					LocalDate.of( 2004, 8, 1 ), LocalDateTime.of( 2004, 8, 1, 12, 0 ), manning ) );
			session.persist( new Book( "9781617290459", "Java Persistence with Hibernate", null, 1000,
					LocalDate.of( 2015, 9, 1 ), LocalDateTime.of( 2015, 9, 1, 12, 0 ), manning ) );
			session.persist( new Book( "9780131889988", "Jakarta Data Guide", null, 250,
					LocalDate.of( 2099, 1, 1 ), LocalDateTime.of( 2025, 1, 1, 12, 0 ), eclipse ) );
		} );
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		String isbn;
		String title;
		String subtitle;
		int pages;
		LocalDate publishedOn;
		LocalDateTime indexedAt;
		@ManyToOne
		Publisher publisher;

		Book() {
		}

		Book(
				String isbn,
				String title,
				String subtitle,
				int pages,
				LocalDate publishedOn,
				LocalDateTime indexedAt,
				Publisher publisher) {
			this.isbn = isbn;
			this.title = title;
			this.subtitle = subtitle;
			this.pages = pages;
			this.publishedOn = publishedOn;
			this.indexedAt = indexedAt;
			this.publisher = publisher;
		}
	}

	@Entity(name = "Publisher")
	public static class Publisher {
		@Id
		Integer id;
		String name;

		Publisher() {
		}

		Publisher(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
