/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.caching;

import java.util.List;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				CachingWithBatchAndFetchModeSelectTest.Category.class
		},
		integrationSettings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
		}

)
@JiraKey(value = "HHH-16230")
public class CachingWithBatchAndFetchModeSelectTest {

	public final static String CATEGORY_A_NAME = "A";
	public final static String CATEGORY_B_NAME = "B";
	public final static String CATEGORY_C_NAME = "C";

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Category categoryA = new Category( CATEGORY_A_NAME );
					entityManager.persist( categoryA );

					Category categoryB = new Category( CATEGORY_B_NAME, categoryA );
					entityManager.persist( categoryB );

					Category categoryC = new Category( CATEGORY_C_NAME );
					entityManager.persist( categoryC );
				}
		);
	}

	@Test
	public void testSelectAll(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					List<Category> categories =
							entityManager.createQuery( "Select c from Category c order by c.name", Category.class )
									.getResultList();

					Category categoryA = categories.get( 0 );

					assertThat( categoryA.getName() ).isEqualTo( CATEGORY_A_NAME );
					assertThat( categoryA.getParentCategory() ).isNull();

					Category categoryB = categories.get( 1 );
					Category parentCategory = categoryB.getParentCategory();

					assertThat( categoryB.getName() ).isEqualTo( CATEGORY_B_NAME );
					assertThat( parentCategory ).isNotNull();
					assertThat( parentCategory ).isEqualTo( categoryA );

					Category categoryC = categories.get( 2 );

					assertThat( categoryC.getName() ).isEqualTo( CATEGORY_C_NAME );
					assertThat( categoryC.getParentCategory() ).isNull();
				}
		);
	}

	@Entity(name = "Category")
	@BatchSize(size = 500)
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Category {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne
		@Fetch(value = FetchMode.SELECT)
		private Category parentCategory;

		public Category() {
		}

		public Category(String name) {
			this.name = name;
		}

		public Category(String name, Category parentCategory) {
			this.name = name;
			this.parentCategory = parentCategory;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Category getParentCategory() {
			return parentCategory;
		}
	}

}
