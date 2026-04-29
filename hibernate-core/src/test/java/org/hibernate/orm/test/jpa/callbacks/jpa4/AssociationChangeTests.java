/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.jpa4;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListener;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreUpdate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {
		AssociationChangeTests.Vendor.class,
		AssociationChangeTests.Part.class,
		AssociationChangeTests.Product.class,
		AssociationChangeTests.UpdateWatcher.class,
})
@SessionFactory
public class AssociationChangeTests {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void baseline(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var vendor = new Vendor( 1, "A" );
			session.persist( vendor );
		} );

		UpdateWatcher.reset();

		factoryScope.inTransaction( (session) -> {
			var vendor = session.get( Vendor.class, 1 );
			vendor.name = vendor.name + 'a';
		} );

		assertThat( UpdateWatcher.vendorPreUpdates ).hasSize( 1 );
		assertThat( UpdateWatcher.vendorPostUpdates ).hasSize( 1 );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-20374" )
	void testOwnedToOneChange(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var vendor = new Vendor( 1, "A" );
			session.persist( vendor );
			var vendor2 = new Vendor( 2, "Z" );
			session.persist( vendor2 );

			var part1 = new Part( 1, "Widgeadonk", vendor );
			session.persist( part1 );
		} );

		UpdateWatcher.reset();

		factoryScope.inTransaction( (session) -> {
			var part = session.get( Part.class, 1 );
			part.vendor = session.getReference( Vendor.class, 2 );
		} );

		assertThat( UpdateWatcher.partPreUpdates ).hasSize( 1 );
		assertThat( UpdateWatcher.partPostUpdates ).hasSize( 1 );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-20374" )
	void testOwnerElementCollectionChange(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var vendor = new Vendor( 1, "A" );
			session.persist( vendor );
		} );

		UpdateWatcher.reset();

		factoryScope.inTransaction( (session) -> {
			var vendor = session.get( Vendor.class, 1 );
			vendor.superlatives.add( "Amaze balls!" );
		} );

		assertThat( UpdateWatcher.vendorPreUpdates ).hasSize( 1 );
		assertThat( UpdateWatcher.vendorPostUpdates ).hasSize( 1 );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-20374" )
	void testOwnerToManyChange(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var vendor = new Vendor( 1, "A" );
			session.persist( vendor );

			var part1 = new Part( 1, "Widgeadonk", vendor );
			session.persist( part1 );

			var part2 = new Part( 2, "Padankadonk", vendor );
			session.persist( part2 );

			var product = new Product( 1, "stuff" );
			product.parts = new HashSet<>();
			product.parts.add( part1 );
			session.persist( product );
		} );

		UpdateWatcher.reset();
		factoryScope.inTransaction( (session) -> {
			var product = session.get( Product.class, 1 );
			product.parts.add( session.getReference( Part.class, 2 ) );
		} );

		assertThat( UpdateWatcher.productPreUpdates ).hasSize( 1 );
		assertThat( UpdateWatcher.productPostUpdates ).hasSize( 1 );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-20374" )
	void testUnownedToManyChange(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var vendor = new Vendor( 1, "A" );
			session.persist( vendor );

			var part1 = new Part( 1, "Widgeadonk", vendor );
			session.persist( part1 );

			var part2 = new Part( 2, "Padankadonk", null );
			session.persist( part2 );
		} );

		UpdateWatcher.reset();
		factoryScope.inTransaction( (session) -> {
			var vendor = session.get( Vendor.class, 1 );
			var part2 = session.getReference( Part.class, 2 );
			// because `Vendor.parts` is the inverse side of the relationship,
			// this addition should NOT make Vendor dirty and so should NOT
			// trigger an update
			vendor.parts.add( part2 );
		} );

		// ... and therefore, should NOT trigger events
		assertThat( UpdateWatcher.vendorPreUpdates ).hasSize( 0 );
		assertThat( UpdateWatcher.vendorPostUpdates ).hasSize( 0 );
	}

	@Entity
	public static class Vendor {
		@Id
		private Integer id;
		private String name;
		@ElementCollection
		private Set<String> superlatives = new HashSet<>();
		@OneToMany(mappedBy = "vendor")
		private Set<Part> parts = new HashSet<>();

		public Vendor() {
		}

		public Vendor(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity
	public static class Part {
		@Id
		private Integer id;
		private String name;
		@ManyToOne
		@JoinColumn(name = "vendor_fk")
		private Vendor vendor;

		public Part() {
		}

		public Part(Integer id, String name, Vendor vendor) {
			this.id = id;
			this.name = name;
			this.vendor = vendor;
		}
	}

	@Entity
	public static class Product {
		@Id
		private Integer id;
		private String name;
		@OneToMany
		private Set<Part> parts;

		public Product() {
		}

		public Product(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@EntityListener
	public static class UpdateWatcher {
		public static final List<Vendor> vendorPreUpdates = new ArrayList<>();
		public static final List<Vendor> vendorPostUpdates = new ArrayList<>();

		public static final List<Part> partPreUpdates = new ArrayList<>();
		public static final List<Part> partPostUpdates = new ArrayList<>();

		public static final List<Product> productPreUpdates = new ArrayList<>();
		public static final List<Product> productPostUpdates = new ArrayList<>();

		public static void reset() {
			vendorPreUpdates.clear();
			vendorPostUpdates.clear();

			partPreUpdates.clear();
			partPostUpdates.clear();

			productPreUpdates.clear();
			productPostUpdates.clear();
		}

		@PreUpdate
		public void preUpdate(Vendor vendor) {
			vendorPreUpdates.add( vendor );
		}

		@PostUpdate
		public void postUpdate(Vendor vendor) {
			vendorPostUpdates.add( vendor );
		}

		@PreUpdate
		public void preUpdate(Part part) {
			partPreUpdates.add( part );
		}

		@PostUpdate
		public void postUpdate(Part part) {
			partPostUpdates.add( part );
		}

		@PreUpdate
		public void preUpdate(Product product) {
			productPreUpdates.add( product );
		}

		@PostUpdate
		public void postUpdate(Product product) {
			productPostUpdates.add( product );
		}
	}

}
