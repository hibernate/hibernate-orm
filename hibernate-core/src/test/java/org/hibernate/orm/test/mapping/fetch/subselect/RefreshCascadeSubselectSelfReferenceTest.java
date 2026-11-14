package org.hibernate.orm.test.mapping.fetch.subselect;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Refreshing an entity whose collection elements are already managed (and so
 * have no {@code EntityInitializer} in their {@code EntityHolder}) must not
 * fail when the element type declares a {@link FetchMode#SUBSELECT} collection.
 */
@JiraKey("HHH-20689")
@JiraKey("HHH-20761")
@DomainModel(annotatedClasses = {
		RefreshCascadeSubselectSelfReferenceTest.Item.class,
		RefreshCascadeSubselectSelfReferenceTest.SubselectMarker.class,
		RefreshCascadeSubselectSelfReferenceTest.Barcode.class
})
@SessionFactory
public class RefreshCascadeSubselectSelfReferenceTest {

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testRefreshCascade(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Item relatedItem = new Item( 2L );
			final Barcode barcode1 = new Barcode( 201L );
			final Barcode barcode2 = new Barcode( 202L );
			relatedItem.addBarcode( barcode1 );
			relatedItem.addBarcode( barcode2 );

			final Item rootItem = new Item( 1L );
			rootItem.addRelatedItem( relatedItem );

			session.persist( relatedItem );
			session.persist( rootItem );
			session.persist( barcode1 );
			session.persist( barcode2 );
		} );

		scope.inTransaction( session -> {
			final Item rootItem = session.find( Item.class, 1L );
			session.refresh( rootItem );
			assertThat( rootItem.relatedItems ).hasSize( 1 );
			assertThat( rootItem.relatedItems.iterator().next().barcodes ).hasSize( 2 );
		} );
	}

	@Entity(name = "Item")
	static class Item {
		@Id
		Long id;

		@ManyToOne
		Item controllingItem;

		@OneToMany(mappedBy = "controllingItem", cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
		Set<Item> relatedItems = new LinkedHashSet<>();

		@Fetch(FetchMode.SUBSELECT)
		@OneToMany(mappedBy = "item")
		List<SubselectMarker> subselectMarkers = new ArrayList<>();

		@OneToMany(mappedBy = "item", fetch = FetchType.EAGER)
		Set<Barcode> barcodes = new LinkedHashSet<>();

		Item() {
		}

		Item(Long id) {
			this.id = id;
		}

		void addRelatedItem(Item item) {
			relatedItems.add( item );
			item.controllingItem = this;
		}

		void addBarcode(Barcode barcode) {
			barcodes.add( barcode );
			barcode.item = this;
		}
	}

	@Entity(name = "SubselectMarker")
	static class SubselectMarker {
		@Id
		Long id;

		@ManyToOne
		Item item;
	}

	@Entity(name = "Barcode")
	static class Barcode {
		@Id
		Long id;

		@ManyToOne
		Item item;

		Barcode() {
		}

		Barcode(Long id) {
			this.id = id;
		}
	}
}
