/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.restriction;

import jakarta.data.expression.TemporalExpression;
import jakarta.data.metamodel.NavigableAttribute;
import jakarta.data.metamodel.NumericAttribute;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.restrict.Restrict;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.query.specification.SelectionSpecification;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.hibernate.query.Order.asc;
import static org.hibernate.query.restriction.JakartaDataRestriction.from;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
		assertTitles( scope, ISBN.in( List.of( "9781932394153", "9780131889988" ) ),
				"Hibernate in Action",
				"Jakarta Data Guide" );
		assertTitles( scope, PAGES.in( List.of( 250, 400 ) ),
				"Hibernate in Action",
				"Jakarta Data Guide" );
		assertTitles( scope, PAGES.in( PAGES.plus( 0 ) ),
				"Hibernate in Action",
				"Jakarta Data Guide",
				"Java Persistence with Hibernate" );
		assertTitles( scope, TITLE.notContains( "Hibernate" ),
				"Jakarta Data Guide" );
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
		assertTitles( scope, TITLE.length().greaterThan( 20 ),
				"Java Persistence with Hibernate" );
		assertTitles( scope, PAGES.plus( 50 ).lessThan( 500 ),
				"Hibernate in Action",
				"Jakarta Data Guide" );
		assertTitles( scope, TITLE.left( 7 ).equalTo( "Jakarta" ),
				"Jakarta Data Guide" );
		assertTitles( scope, TITLE.right( 9 ).equalTo( "Hibernate" ),
				"Java Persistence with Hibernate" );
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

	private static void persistBooks(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inTransaction( session -> {
			final Publisher manning = new Publisher( 1, "Manning" );
			final Publisher eclipse = new Publisher( 2, "Eclipse Foundation" );
			session.persist( manning );
			session.persist( eclipse );
			session.persist( new Book( "9781932394153", "Hibernate in Action", 400, manning ) );
			session.persist( new Book( "9781617290459", "Java Persistence with Hibernate", 1000, manning ) );
			session.persist( new Book( "9780131889988", "Jakarta Data Guide", 250, eclipse ) );
		} );
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		String isbn;
		String title;
		int pages;
		@ManyToOne
		Publisher publisher;

		Book() {
		}

		Book(String isbn, String title, int pages, Publisher publisher) {
			this.isbn = isbn;
			this.title = title;
			this.pages = pages;
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
