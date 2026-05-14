/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.BooleanAttribute;
import jakarta.persistence.metamodel.ComparableAttribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.NumericAttribute;
import jakarta.persistence.metamodel.TemporalAttribute;
import jakarta.persistence.metamodel.TextAttribute;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@JiraKey("HHH-20363")
@Jpa(annotatedClasses = Jpa4CriteriaApiTest.Article.class)
class Jpa4CriteriaApiTest {

	@Test
	void specializedMetamodelAttributesAndCriteriaPaths(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.persist( new Article(
					1L,
					"Hibernate Criteria",
					145,
					true,
					LocalDate.of( 2026, 1, 15 ),
					Status.PUBLISHED
			) );
			entityManager.persist( new Article(
					2L,
					"Draft Criteria",
					12,
					false,
					LocalDate.of( 2024, 4, 1 ),
					Status.DRAFT
			) );
		} );

		scope.inEntityManager( entityManager -> {
			final CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			final TextAttribute<Article> titleAttribute = Jpa4CriteriaApiTest_.Article_.title;
			final NumericAttribute<Article, Integer> pagesAttribute = Jpa4CriteriaApiTest_.Article_.pages;
			final BooleanAttribute<Article> publishedAttribute = Jpa4CriteriaApiTest_.Article_.published;
			final TemporalAttribute<Article, LocalDate> publishedOnAttribute = Jpa4CriteriaApiTest_.Article_.publishedOn;
			final ComparableAttribute<Article, Status> statusAttribute = Jpa4CriteriaApiTest_.Article_.status;

			final var textLiteral = builder.stringLiteral( "Hibernate" );
			final var temporalLiteral = builder.temporalLiteral( LocalDate.of( 2025, 1, 1 ) );
			final var booleanLiteral = builder.booleanLiteral( false );

			final var query = builder.createQuery( Article.class );
			final var article = query.from( Article.class );
			final var title = article.get( titleAttribute );
			final var pages = article.get( pagesAttribute );
			final var published = article.get( publishedAttribute );
			final var publishedOn = article.get( publishedOnAttribute );
			final var status = article.get( statusAttribute );

			query.where(
					builder.and(
							title.contains( "Hibernate" ),
							title.like( textLiteral.append( "%" ) ),
							pages.gt( 100 ),
							published.not().equalTo( booleanLiteral ),
							publishedOn.greaterThan( temporalLiteral ),
							status.greaterThan( Status.DRAFT )
					)
			);

			final List<Article> results = entityManager.createQuery( query ).getResultList();
			assertEquals( 1, results.size() );
			assertEquals( "Hibernate Criteria", results.get( 0 ).title );
		} );

		final EntityType<Article> articleType =
				scope.getEntityManagerFactory().getMetamodel()
						.entity( Article.class );
		assertInstanceOf( NumericAttribute.class, articleType.getSingularAttribute( "id" ) );
		assertInstanceOf( TextAttribute.class, articleType.getSingularAttribute( "title" ) );
		assertInstanceOf( NumericAttribute.class, articleType.getSingularAttribute( "pages" ) );
		assertInstanceOf( BooleanAttribute.class, articleType.getSingularAttribute( "published" ) );
		assertInstanceOf( TemporalAttribute.class, articleType.getSingularAttribute( "publishedOn" ) );
		assertInstanceOf( ComparableAttribute.class, articleType.getSingularAttribute( "status" ) );
	}

	@Entity(name = "Jpa4CriteriaArticle")
	public static class Article {
		@Id
		private Long id;

		private String title;

		private int pages;

		private boolean published;

		private LocalDate publishedOn;

		private Status status;

		public Article() {
		}

		public Article(Long id, String title, int pages, boolean published, LocalDate publishedOn, Status status) {
			this.id = id;
			this.title = title;
			this.pages = pages;
			this.published = published;
			this.publishedOn = publishedOn;
			this.status = status;
		}
	}

	public enum Status {
		DRAFT,
		PUBLISHED
	}
}
