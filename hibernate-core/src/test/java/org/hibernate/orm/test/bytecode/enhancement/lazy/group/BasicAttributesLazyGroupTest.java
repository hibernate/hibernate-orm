package org.hibernate.orm.test.bytecode.enhancement.lazy.group;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(BytecodeEnhancerRunner.class)
@TestForIssue(jiraKey = "HHH-14874")
public class BasicAttributesLazyGroupTest extends BaseCoreFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Review.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, false );
		configuration.setProperty( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, true );
	}

	@Before
	public void setUp() {
		inTransaction(
				session -> {
					Review review = new Review();
					review.setComment( "My first review" );
					review.setRating( Rating.ONE );
					session.save( review );
				}
		);
	}

	@Test
	public void testLoad() {
		inTransaction( session -> {
			final Review review = session.load( Review.class, 1L );

			assertFalse( Hibernate.isPropertyInitialized( review, "rating" ) );
			assertFalse( Hibernate.isPropertyInitialized( review, "comment" ) );

			assertThat( review.getRating(), is( Rating.ONE ) );

			assertTrue( Hibernate.isPropertyInitialized( review, "rating" ) );
			assertFalse( Hibernate.isPropertyInitialized( review, "comment" ) );
		} );
	}

	@Test
	public void testLoad2() {
		inTransaction( session -> {
			final Review review = session.load( Review.class, 1L );

			assertFalse( Hibernate.isPropertyInitialized( review, "rating" ) );
			assertFalse( Hibernate.isPropertyInitialized( review, "comment" ) );

			assertThat( review.getComment(), is( "My first review" ) );

			assertTrue( Hibernate.isPropertyInitialized( review, "comment" ) );
			assertFalse( Hibernate.isPropertyInitialized( review, "rating" ) );
		} );
	}

	@Test
	public void testLoad3() {
		inTransaction( session -> {
			final Review review = session.load( Review.class, 1L );

			assertThat( review.getComment(), is( "My first review" ) );
			assertThat( review.getRating(), is( Rating.ONE ) );
		} );
	}

	@Test
	public void testLoad4() {
		inTransaction( session -> {
			final Review review = session.load( Review.class, 1L );
			assertThat( review.getRating(), is( Rating.ONE ) );
			assertThat( review.getComment(), is( "My first review" ) );
		} );
	}

	@Test
	public void testHql() {
		inTransaction( session -> {
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
