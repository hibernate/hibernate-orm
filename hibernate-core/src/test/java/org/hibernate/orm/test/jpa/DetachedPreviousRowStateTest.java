/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Subgraph;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Réda Housni Alaoui
 */
@Jpa(annotatedClasses = {
		DetachedPreviousRowStateTest.Version.class,
		DetachedPreviousRowStateTest.Product.class,
		DetachedPreviousRowStateTest.Description.class,
		DetachedPreviousRowStateTest.LocalizedDescription.class
})
class DetachedPreviousRowStateTest {

	@BeforeEach
	void setupData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			Product product = new Product();
			em.persist( product );

			Description description = new Description( product );
			em.persist( description );

			LocalizedDescription englishDescription = new LocalizedDescription( description );
			em.persist( englishDescription );
			LocalizedDescription frenchDescription = new LocalizedDescription( description );
			em.persist( frenchDescription );
		} );
	}

	@AfterEach
	void cleanupData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			em.createQuery( "delete from org.hibernate.orm.test.jpa.DetachedPreviousRowStateTest$LocalizedDescription l" ).executeUpdate();
			em.createQuery( "delete from org.hibernate.orm.test.jpa.DetachedPreviousRowStateTest$Description d" ).executeUpdate();
			em.createQuery( "delete from org.hibernate.orm.test.jpa.DetachedPreviousRowStateTest$Product p" ).executeUpdate();
		} );
	}

	@Test
	@JiraKey(value = "HHH-18719")
	void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<LocalizedDescription> query = cb.createQuery( LocalizedDescription.class );
			Root<LocalizedDescription> root = query.from( LocalizedDescription.class );
			query.select( root );

			EntityGraph<LocalizedDescription> localizedDescriptionGraph =
					em.createEntityGraph( LocalizedDescription.class );
			Subgraph<Object> descriptionGraph = localizedDescriptionGraph.addSubgraph( "description" );
			Subgraph<Object> productGraph = descriptionGraph.addSubgraph( "product" );
			productGraph.addSubgraph( "versions" );

			AtomicInteger resultCount = new AtomicInteger();

			em
					.createQuery( query )
					.setHint( "jakarta.persistence.loadgraph", localizedDescriptionGraph )
					.getResultStream()
					.forEach( localizedDescription -> {
						resultCount.incrementAndGet();

						assertThat( em.contains( localizedDescription.description.product ) )
								.withFailMessage( "'localizedDescription.description.product' is detached" )
								.isTrue();
						assertThat( Hibernate.isInitialized( localizedDescription.description.product ) )
								.withFailMessage( "'localizedDescription.description.product' is not initialized" )
								.isTrue();

						em.flush();
						em.clear();
					} );

			assertThat( resultCount.get() ).isEqualTo( 2 );
		} );
	}

	@Entity
	@Table(name = "version_tbl")
	public static class Version {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Product product;
	}

	@Entity
	@Table(name = "product_tbl")
	public static class Product {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private long id;

		@OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
		private final List<Version> versions = new ArrayList<>();
	}

	@Entity
	@Table(name = "description_tbl")
	public static class Description {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private long id;

		@OneToOne(fetch = FetchType.LAZY)
		private Product product;

		public Description() {
		}

		public Description(Product product) {
			this.product = product;
		}
	}

	@Entity
	@Table(name = "localized_description_tbl")
	public static class LocalizedDescription {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Description description;

		public LocalizedDescription() {
		}

		public LocalizedDescription(Description description) {
			this.description = description;
		}
	}

}
