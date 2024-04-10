package org.hibernate.orm.test.caching;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Jpa(
		annotatedClasses = {
				CachingBatchLoadNoProxiesAndCircularRelationshipTest.Category.class,
				CachingBatchLoadNoProxiesAndCircularRelationshipTest.CategoryHolder.class
		},
		integrationSettings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
		}
)
@JiraKey("HHH-17918")
public class CachingBatchLoadNoProxiesAndCircularRelationshipTest {

	@BeforeAll
	public void setupEntities(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Category[] categories = new Category[ 5 ];
					
					for (int i=0; i<categories.length; i++) {
						categories[ i ] = new Category( i );
						entityManager.persist( categories[ i ] );
					}
					
					// Chain-link the categories (#n points to #n+1)
					for (int i=0; i<categories.length-1; i++) {
						categories[ i ].nextCategory = categories[ i+1 ];
					}
					// And chain the last category back to the first one
					categories[ categories.length-1 ].nextCategory = categories[ 0 ];
					
					// Create an holder object so when loading it we trigger the load of two categories
					CategoryHolder holder = new CategoryHolder( 0 );
					holder.leftCategory = categories[ 0 ];
					holder.rightCategory = categories[ 3 ];
					entityManager.persist( holder );
				}
		);
	}

	@Test
	public void recursiveBatchLoadingWithCircularRelationship(EntityManagerFactoryScope scope) {
		// Set the state of the 2nd-level cache so it contains #1 (and potentially others) but not #0 or #3
		scope.getEntityManagerFactory().getCache().evict(Category.class);
		
		scope.inTransaction(
				entityManager -> {
					entityManager.getReference( Category.class, 1 );
				}
		);
		
		scope.getEntityManagerFactory().getCache().evict( Category.class, 0 );
		scope.getEntityManagerFactory().getCache().evict( Category.class, 3 );
		
		// This fails with: jakarta.persistence.EntityNotFoundException: Unable to find Category with id 0
		scope.inEntityManager(
				entityManager -> {
					List<CategoryHolder> categories =
							entityManager.createQuery( "Select o from CategoryHolder o", CategoryHolder.class )
									.getResultList();
					
					Hibernate.initialize(categories.get( 0 ));
				}
		);
	}

	@Proxy(lazy = false)
	@Entity(name = "Category")
	@BatchSize(size = 10)
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Category {
		@Id
		private Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(value = FetchMode.SELECT)
		private Category nextCategory;

		public Category() {
		}
		
		public Category(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Category getNextCategory() {
			return nextCategory;
		}

		public void setNextCategory(Category nextCategory) {
			this.nextCategory = nextCategory;
		}
	}

	@Entity(name = "CategoryHolder")
	public static class CategoryHolder {
		@Id
		private Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(value = FetchMode.SELECT)
		private Category leftCategory;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(value = FetchMode.SELECT)
		private Category rightCategory;

		public CategoryHolder() {
		}
		
		public CategoryHolder(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Category getLeftCategory() {
			return leftCategory;
		}

		public void setLeftCategory(Category leftCategory) {
			this.leftCategory = leftCategory;
		}

		public Category getRightCategory() {
			return rightCategory;
		}

		public void setRightCategory(Category rightCategory) {
			this.rightCategory = rightCategory;
		}
	}
}
