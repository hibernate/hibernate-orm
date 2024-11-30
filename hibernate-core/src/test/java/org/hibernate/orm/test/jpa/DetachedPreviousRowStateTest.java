/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Subgraph;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Hibernate;
import org.hibernate.jpa.SpecHints;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
			em.createQuery( "delete from LocalizedDescription l" ).executeUpdate();
			em.createQuery( "delete from Description d" ).executeUpdate();
			em.createQuery( "delete from Product p" ).executeUpdate();
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

			em.createQuery( query )
					.setHint( SpecHints.HINT_SPEC_LOAD_GRAPH, localizedDescriptionGraph )
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

	@Entity(name = "Version")
	@Table(name = "version_tbl")
	public static class Version {

		@Id
		@GeneratedValue
		private long id;

		private String description;

		@ManyToOne(fetch = FetchType.LAZY)
		private Product product;
	}

	@Entity(name = "Product")
	@Table(name = "product_tbl")
	public static class Product {
		@Id
		@GeneratedValue
		private long id;

		private String description;

		@OneToMany(mappedBy = "product")
		private final List<Version> versions = new ArrayList<>();
	}

	@Entity(name = "Description")
	@Table(name = "description_tbl")
	public static class Description {
		@Id
		@GeneratedValue
		private long id;

		private String description;

		@OneToOne(fetch = FetchType.LAZY)
		private Product product;

		public Description() {
		}

		public Description(Product product) {
			this.product = product;
		}
	}

	@Entity(name = "LocalizedDescription")
	@Table(name = "localized_description_tbl")
	public static class LocalizedDescription {

		@Id
		@GeneratedValue
		private long id;

		private String localizedDescription;

		@ManyToOne(fetch = FetchType.LAZY)
		private Description description;

		public LocalizedDescription() {
		}

		public LocalizedDescription(Description description) {
			this.description = description;
		}
	}

}
