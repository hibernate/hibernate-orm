/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.group;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey("HHH-14874")
@DomainModel(
		annotatedClasses = {
				BasicAttributesLazyGroupTest.Review.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
public class BasicAttributesLazyGroupTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Review review = new Review();
					review.setComment( "My first review" );
					review.setRating( Rating.ONE );
					session.persist( review );
				}
		);
	}

	@AfterAll
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createMutationQuery( "delete Review" ).executeUpdate()
		);
	}

	@Test
	public void testLoad(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Review review = session.getReference( Review.class, 1L );

			assertFalse( Hibernate.isPropertyInitialized( review, "rating" ) );
			assertFalse( Hibernate.isPropertyInitialized( review, "comment" ) );

			assertThat( review.getRating(), is( Rating.ONE ) );

			assertTrue( Hibernate.isPropertyInitialized( review, "rating" ) );
			assertFalse( Hibernate.isPropertyInitialized( review, "comment" ) );
		} );
	}

	@Test
	public void testLoad2(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Review review = session.getReference( Review.class, 1L );

			assertFalse( Hibernate.isPropertyInitialized( review, "rating" ) );
			assertFalse( Hibernate.isPropertyInitialized( review, "comment" ) );

			assertThat( review.getComment(), is( "My first review" ) );

			assertTrue( Hibernate.isPropertyInitialized( review, "comment" ) );
			assertFalse( Hibernate.isPropertyInitialized( review, "rating" ) );
		} );
	}

	@Test
	public void testLoad3(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Review review = session.getReference( Review.class, 1L );

			assertThat( review.getComment(), is( "My first review" ) );
			assertThat( review.getRating(), is( Rating.ONE ) );
		} );
	}

	@Test
	public void testLoad4(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Review review = session.getReference( Review.class, 1L );
			assertThat( review.getRating(), is( Rating.ONE ) );
			assertThat( review.getComment(), is( "My first review" ) );
		} );
	}

	@Test
	public void testHql(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Review> reviews = session.createQuery( "select r from Review r" ).list();
			assertThat( reviews.size(), is( 1 ) );

			final Review review = reviews.get( 0 );
			assertFalse( Hibernate.isPropertyInitialized( review, "rating" ) );
			assertFalse( Hibernate.isPropertyInitialized( review, "comment" ) );

			assertThat( review.getRating(), is( Rating.ONE ) );

			assertTrue( Hibernate.isPropertyInitialized( review, "rating" ) );
			assertFalse( Hibernate.isPropertyInitialized( review, "comment" ) );
		} );
	}


	@Entity(name = "Review")
	public static class Review {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@Enumerated
		@Basic(fetch = FetchType.LAZY)
		private Rating rating;

		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("comment")
		@Column(name = "review_comment")
		private String comment;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Rating getRating() {
			return rating;
		}

		public void setRating(Rating rating) {
			this.rating = rating;
		}

		public String getComment() {
			return comment;
		}

		public void setComment(String comment) {
			this.comment = comment;
		}
	}

	enum Rating {
		ONE, TWO
	}
}
