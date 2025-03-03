/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria.internal.hhh14916;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey(value = "HHH-14916")
@Jpa(
		annotatedClasses = { Author.class, Book.class, Chapter.class }
)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
public class HHH14916Test {

	@BeforeEach
	public void before(EntityManagerFactoryScope scope) {
		populateData( scope );
	}

	@Test
	public void testJoinOnFetchNoExceptionThrow(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Author> query = builder.createQuery( Author.class );

			final Root<Author> root = query.from( Author.class );
			final ListJoin<Author, Book> authorBookJoin = (ListJoin) root.fetch( "books", JoinType.LEFT );

			final ListJoin<Book, Chapter> bookChapterJoin = authorBookJoin.joinList( "chapters", JoinType.LEFT );

			final Predicate finalPredicate = builder.equal( bookChapterJoin.get( "name" ), "Overview of HTTP" );
			query.where( finalPredicate );

			Author author = entityManager.createQuery( query ).getSingleResult();

			assertEquals( "David Gourley", author.name );
			assertEquals( "HTTP Definitive guide", author.books.get( 0 ).name );
			assertEquals( "Overview of HTTP", author.books.get( 0 ).chapters.get( 0 ).name );
		} );
	}

	public void populateData(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			// Insert data
			Chapter chapter = new Chapter();
			chapter.name = "Overview of HTTP";

			Book book = new Book();
			book.name = "HTTP Definitive guide";

			Author author = new Author();
			author.name = "David Gourley";

			book.chapters.add( chapter );
			author.books.add( book );

			chapter.book = book;
			book.author = author;

			entityManager.persist( author );
		} );
	}
}
