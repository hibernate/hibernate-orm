package org.hibernate.orm.test.caching;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		CachingBatchLoadNoProxiesAndCircularRelationshipTest.Category.class,
		CachingBatchLoadNoProxiesAndCircularRelationshipTest.CategoryHolder.class,
} )
@SessionFactory
@ServiceRegistry( settings = @Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ) )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17918" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17983" )
public class CachingBatchLoadNoProxiesAndCircularRelationshipTest {
	private static final int NUMBER_OF_CATEGORIES = 5;

	@Test
	public void recursiveBatchLoadingWithCircularRelationship(SessionFactoryScope scope) {
		// Set the state of the 2nd-level cache, so it contains #1 (and potentially others) but not #0 or #3
		scope.getSessionFactory().getCache().evict( Category.class );
		scope.inTransaction( session -> session.getReference( Category.class, 1 ) );
		scope.getSessionFactory().getCache().evict( Category.class, 0 );
		scope.getSessionFactory().getCache().evict( Category.class, 3 );

		scope.inSession( session -> {
			final CategoryHolder result = session.createQuery(
					"from CategoryHolder where id = 0",
					CategoryHolder.class
			).getSingleResult();
			Category category = result.getLeftCategory();
			for ( int i = 0; i < NUMBER_OF_CATEGORIES; i++ ) {
				assertThat( category ).matches( Hibernate::isInitialized, "Category was not initialized" )
						.extracting( Category::getId )
						.isEqualTo( i );
				if ( i == 3 ) {
					assertThat( category ).isSameAs( result.getRightCategory() );
				}
				else if ( i == NUMBER_OF_CATEGORIES - 1 ) {
					assertThat( category.getNextCategory() ).isSameAs( result.getLeftCategory() );
				}
				category = category.getNextCategory();
			}
		} );
	}

	@Test
	public void recursiveBatchLoadingSameQueryTest(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evict( Category.class );
		scope.inSession( session -> {
			final CategoryHolder result = session.find( CategoryHolder.class, 1 );
			Category category = result.getLeftCategory();
			for ( int i = 0; i < NUMBER_OF_CATEGORIES; i++ ) {
				assertThat( category ).matches( Hibernate::isInitialized, "Category was not initialized" )
						.extracting( Category::getId )
						.isEqualTo( i );
				if ( i == NUMBER_OF_CATEGORIES - 1 ) {
					assertThat( category ).isSameAs( result.getRightCategory() );
					assertThat( category.getNextCategory() ).isSameAs( result.getLeftCategory() );
				}
				category = category.getNextCategory();
			}
		} );
	}

	@BeforeAll
	public void setupEntities(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Category[] categories = new Category[NUMBER_OF_CATEGORIES];
			for ( int i = 0; i < categories.length; i++ ) {
				categories[i] = new Category( i, "category_" + i );
				session.persist( categories[i] );
			}
			// Chain-link the categories (#n points to #n+1, last one points to #0)
			for ( int i = 0; i < categories.length - 1; i++ ) {
				categories[i].setNextCategory( categories[i + 1] );
			}
			categories[categories.length - 1].nextCategory = categories[0];
			final CategoryHolder holder1 = new CategoryHolder( 0 );
			holder1.leftCategory = categories[0];
			holder1.rightCategory = categories[3];
			session.persist( holder1 );
			final CategoryHolder holder2 = new CategoryHolder( 1 );
			holder2.leftCategory = categories[0];
			holder2.rightCategory = categories[4];
			session.persist( holder2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from CategoryHolder" ).executeUpdate();
			session.createQuery( "from Category", Category.class )
					.getResultList()
					.forEach( c -> c.setNextCategory( null ) );
			session.createMutationQuery( "delete from Category" ).executeUpdate();
		} );
	}

	@Proxy( lazy = false )
	@Entity( name = "Category" )
	@BatchSize( size = 10 )
	@Cacheable
	@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
	static class Category {
		@Id
		private Integer id;

		private String name;

		@ManyToOne( fetch = FetchType.LAZY )
		@Fetch( value = FetchMode.SELECT )
		private Category nextCategory;

		public Category() {
		}

		public Category(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public Category getNextCategory() {
			return nextCategory;
		}

		public void setNextCategory(Category nextCategory) {
			this.nextCategory = nextCategory;
		}

		public String getName() {
			return name;
		}

		@PostLoad
		public void postLoad() {
			assertThat( name ).isNotNull().isEqualTo( "category_" + id );
			assertThat( nextCategory.getId() ).isNotNull();
			// note : nextCategory.name will be null here since the instance will not have been initialized yet
		}
	}

	@Entity( name = "CategoryHolder" )
	static class CategoryHolder {
		@Id
		private Integer id;

		@ManyToOne( fetch = FetchType.LAZY )
		@Fetch( value = FetchMode.SELECT )
		private Category leftCategory;

		@ManyToOne( fetch = FetchType.LAZY )
		@Fetch( value = FetchMode.SELECT )
		private Category rightCategory;

		public CategoryHolder() {
		}

		public CategoryHolder(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public Category getLeftCategory() {
			return leftCategory;
		}

		public Category getRightCategory() {
			return rightCategory;
		}
	}
}
