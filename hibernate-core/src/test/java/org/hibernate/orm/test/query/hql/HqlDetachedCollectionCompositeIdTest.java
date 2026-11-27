package org.hibernate.orm.test.query.hql;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.hibernate.collection.spi.PersistentCollection;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for selecting a collection belonging to an entity
 * with a composite identifier.
 *
 * @author Eduardo Ramirez
 */
@DomainModel( annotatedClasses = {
		HqlDetachedCollectionCompositeIdTest.Lot.class,
		HqlDetachedCollectionCompositeIdTest.Ticket.class,
		HqlDetachedCollectionCompositeIdTest.Batch.class,
		HqlDetachedCollectionCompositeIdTest.BatchItem.class,
} )
@SessionFactory
@JiraKey( "HHH-21268" )
public class HqlDetachedCollectionCompositeIdTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Lot firstLot = new Lot( new LotId( 1, 100 ) );
			firstLot.getLabels().add( "organic" );
			firstLot.getLabels().add( "fair trade" );
			session.persist( firstLot );

			final Lot secondLot = new Lot( new LotId( 1, 101 ) );
			secondLot.getLabels().add( "local" );
			session.persist( secondLot );

			final Ticket ticket = new Ticket( 7, 42 );
			ticket.getTags().add( "billing" );
			ticket.getTags().add( "urgent" );
			session.persist( ticket );

			final Batch batch = new Batch( new BatchId( 3, 9 ) );
			batch.getItems().add( new BatchItem( 1, "first" ) );
			batch.getItems().add( new BatchItem( 2, "second" ) );
			session.persist( batch );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testEmbeddedIdCollectionSelection(SessionFactoryScope scope) {
		final List<Collection<String>> labelLists = scope.fromTransaction(
				session -> session.createSelectionQuery(
						"select lot.labels from Hhh21268Lot lot order by lot.id.lotNumber",
						collectionType( String.class )
				).getResultList()
		);

		assertThat( labelLists ).hasSize( 2 );
		assertDetachedList( labelLists.get( 0 ) );
		assertThat( labelLists.get( 0 ) ).containsExactlyInAnyOrder( "organic", "fair trade" );
		assertDetachedList( labelLists.get( 1 ) );
		assertThat( labelLists.get( 1 ) ).containsExactly( "local" );
	}

	@Test
	public void testIdClassCollectionSelection(SessionFactoryScope scope) {
		final Collection<String> tags = scope.fromTransaction(
				session -> session.createSelectionQuery(
						"select ticket.tags from Hhh21268Ticket ticket",
						collectionType( String.class )
				).getSingleResult()
		);

		assertDetachedSet( tags );
		assertThat( tags ).containsExactlyInAnyOrder( "billing", "urgent" );
	}

	@Test
	public void testEmbeddedIdOneToManySelection(SessionFactoryScope scope) {
		final Collection<BatchItem> items = scope.fromTransaction(
				session -> session.createSelectionQuery(
						"select batch.items from Hhh21268Batch batch",
						collectionType( BatchItem.class )
				).getSingleResult()
		);

		assertDetachedList( items );
		assertThat( items )
				.extracting( BatchItem::getName )
				.containsExactlyInAnyOrder( "first", "second" );
	}

	@SuppressWarnings("unchecked")
	private static @NonNull <T> Class<Collection<T>> collectionType(Class<T> elementType) {
		return (Class<Collection<T>>) (Class) Collection.class;
	}

	private static void assertDetachedSet(Collection<?> collection) {
		assertThat( collection ).isInstanceOf( Set.class );
		assertThat( collection ).isNotInstanceOf( PersistentCollection.class );
	}

	private static void assertDetachedList(Collection<?> collection) {
		assertThat( collection ).isInstanceOf( List.class );
		assertThat( collection ).isNotInstanceOf( PersistentCollection.class );
	}

	@Embeddable
	public static class LotId implements Serializable {
		@Column( name = "site_id" )
		private Integer siteId;

		@Column( name = "lot_number" )
		private Integer lotNumber;

		protected LotId() {
		}

		private LotId(Integer siteId, Integer lotNumber) {
			this.siteId = siteId;
			this.lotNumber = lotNumber;
		}

		public Integer getSiteId() {
			return siteId;
		}

		public Integer getLotNumber() {
			return lotNumber;
		}

		@Override
		public boolean equals(Object other) {
			if ( this == other ) {
				return true;
			}
			if ( !( other instanceof LotId lotId ) ) {
				return false;
			}
			return Objects.equals( siteId, lotId.siteId )
					&& Objects.equals( lotNumber, lotId.lotNumber );
		}

		@Override
		public int hashCode() {
			return Objects.hash( siteId, lotNumber );
		}
	}

	@Entity( name = "Hhh21268Lot" )
	@Table( name = "hhh21268_lot" )
	public static class Lot {
		@EmbeddedId
		private LotId id;

		@ElementCollection
		@CollectionTable( name = "hhh21268_lot_label",
				joinColumns = {
						@JoinColumn( name = "site_id" ),
						@JoinColumn( name = "lot_number" )
				} )
		@Column( name = "label" )
		private List<String> labels = new ArrayList<>();

		protected Lot() {
		}

		private Lot(LotId id) {
			this.id = id;
		}

		public LotId getId() {
			return id;
		}

		public List<String> getLabels() {
			return labels;
		}
	}

	public record TicketId(Integer tenantId, Integer ticketNumber) implements Serializable {
	}

	@Entity( name = "Hhh21268Ticket" )
	@Table( name = "hhh21268_ticket" )
	@IdClass( TicketId.class )
	public static class Ticket {
		@Id
		@Column( name = "tenant_id" )
		private Integer tenantId;

		@Id
		@Column( name = "ticket_number" )
		private Integer ticketNumber;

		@ElementCollection
		@CollectionTable( name = "hhh21268_ticket_tag",
				joinColumns = {
						@JoinColumn( name = "tenant_id" ),
						@JoinColumn( name = "ticket_number" )
				} )
		@Column( name = "tag" )
		private Set<String> tags = new HashSet<>();

		protected Ticket() {
		}

		private Ticket(Integer tenantId, Integer ticketNumber) {
			this.tenantId = tenantId;
			this.ticketNumber = ticketNumber;
		}

		public Set<String> getTags() {
			return tags;
		}
	}

	@Embeddable
	public static class BatchId implements Serializable {
		@Column( name = "plant_id" )
		private Integer plantId;

		@Column( name = "batch_number" )
		private Integer batchNumber;

		protected BatchId() {
		}

		private BatchId(Integer plantId, Integer batchNumber) {
			this.plantId = plantId;
			this.batchNumber = batchNumber;
		}

		public Integer getPlantId() {
			return plantId;
		}

		public Integer getBatchNumber() {
			return batchNumber;
		}

		@Override
		public boolean equals(Object other) {
			if ( this == other ) {
				return true;
			}
			if ( !( other instanceof BatchId batchId ) ) {
				return false;
			}
			return Objects.equals( plantId, batchId.plantId )
					&& Objects.equals( batchNumber, batchId.batchNumber );
		}

		@Override
		public int hashCode() {
			return Objects.hash( plantId, batchNumber );
		}
	}

	@Entity( name = "Hhh21268Batch" )
	@Table( name = "hhh21268_batch" )
	public static class Batch {
		@EmbeddedId
		private BatchId id;

		@OneToMany( cascade = CascadeType.PERSIST )
		@JoinColumns( {
				@JoinColumn( name = "plant_id", referencedColumnName = "plant_id" ),
				@JoinColumn( name = "batch_number", referencedColumnName = "batch_number" )
		} )
		private List<BatchItem> items = new ArrayList<>();

		protected Batch() {
		}

		private Batch(BatchId id) {
			this.id = id;
		}

		public BatchId getId() {
			return id;
		}

		public List<BatchItem> getItems() {
			return items;
		}
	}

	@Entity( name = "Hhh21268BatchItem" )
	@Table( name = "hhh21268_batch_item" )
	public static class BatchItem {
		@Id
		private Integer id;

		@Column( name = "item_name" )
		private String name;

		protected BatchItem() {
		}

		private BatchItem(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
