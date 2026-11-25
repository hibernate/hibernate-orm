package org.hibernate.orm.test.softdelete;

import org.hibernate.Hibernate;
import org.hibernate.annotations.PartitionKey;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SoftDelete;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import static jakarta.persistence.FetchType.LAZY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A delete or update of an entity whose partition key is not loaded, because
 * it is a lazy attribute which was never fetched, or because the entity is
 * read-only and has no snapshot, is not restricted by a wrong partition key.
 */
@DomainModel(annotatedClasses = {
		PartitionKeyNotLoadedTest.LazyShipment.class,
		PartitionKeyNotLoadedTest.LazyParcel.class,
		PartitionKeyNotLoadedTest.LazyCrate.class,
		PartitionKeyNotLoadedTest.Pallet.class,
		PartitionKeyNotLoadedTest.Container.class,
		PartitionKeyNotLoadedTest.CustomDeleteContainer.class
})
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true)
@JiraKey("HHH-21262")
class PartitionKeyNotLoadedTest {

	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new LazyShipment( 1, "eu" ) );
			session.persist( new LazyParcel( 1, "eu" ) );
			session.persist( new LazyCrate( 1, "eu" ) );
			session.persist( new Pallet( 1, "eu" ) );
			session.persist( new Container( 1, "eu" ) );
			session.persist( new CustomDeleteContainer( 1, "eu" ) );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void softDeleteWithUnfetchedPartitionKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var shipment = session.find( LazyShipment.class, 1 );
			assertThat( Hibernate.isPropertyInitialized( shipment, "region" ) ).isFalse();
			session.remove( shipment );
		} );
		assertSoftDeleted( scope, LazyShipment.class, "lazy_soft_shipment", 1 );
	}

	@Test
	void softDeleteWithUnfetchedSecondaryTablePartitionKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var parcel = session.find( LazyParcel.class, 1 );
			assertThat( Hibernate.isPropertyInitialized( parcel, "region" ) ).isFalse();
			session.remove( parcel );
		} );
		assertSoftDeleted( scope, LazyParcel.class, "lazy_soft_parcel", 1 );
	}

	@Test
	void deleteWithUnfetchedPartitionKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var crate = session.find( LazyCrate.class, 1 );
			assertThat( Hibernate.isPropertyInitialized( crate, "region" ) ).isFalse();
			session.remove( crate );
		} );
		scope.inTransaction( session -> assertNull( session.find( LazyCrate.class, 1 ) ) );
	}

	@Test
	void updateWithUnfetchedPartitionKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var crate = session.find( LazyCrate.class, 1 );
			assertThat( Hibernate.isPropertyInitialized( crate, "region" ) ).isFalse();
			crate.label = "fragile";
		} );
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
						"select label, region from lazy_crate where id = 1", Object[].class ).getSingleResult() )
				.containsExactly( "fragile", "eu" ) );
	}

	@Test
	void softDeleteReadOnlyWithChangedPartitionKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setDefaultReadOnly( true );
			final var pallet = session.find( Pallet.class, 1 );
			// a read-only entity has no snapshot of its state
			pallet.region = "uk";
			session.remove( pallet );
		} );
		assertSoftDeleted( scope, Pallet.class, "read_only_soft_pallet", 1 );
		scope.inTransaction( session -> assertEquals( "eu", session.createNativeQuery(
				"select region from read_only_soft_pallet where id = 1", String.class ).getSingleResult() ) );
	}

	@Test
	void deleteReadOnlyWithChangedPartitionKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setDefaultReadOnly( true );
			final var container = session.find( Container.class, 1 );
			container.region = "uk";
			session.remove( container );
		} );
		scope.inTransaction( session -> assertNull( session.find( Container.class, 1 ) ) );
	}

	@Test
	void customDeleteReadOnlyWithChangedPartitionKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setDefaultReadOnly( true );
			final var container = session.find( CustomDeleteContainer.class, 1 );
			container.region = "uk";
			session.remove( container );
		} );
		scope.inTransaction( session -> assertNull( session.find( CustomDeleteContainer.class, 1 ) ) );
	}

	@Test
	void customDeleteWithUnfetchedPartitionKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var container = session.find( CustomDeleteContainer.class, 1 );
			assertThat( Hibernate.isPropertyInitialized( container, "region" ) ).isFalse();
			session.remove( container );
		} );
		scope.inTransaction( session -> assertNull( session.find( CustomDeleteContainer.class, 1 ) ) );
	}

	private static void assertSoftDeleted(SessionFactoryScope scope, Class<?> entityClass, String table, int id) {
		scope.inTransaction( session -> {
			assertNull( session.find( entityClass, id ) );
			assertEquals( 1L, session.createNativeQuery( "select count(*) from " + table + " where id = :id", Long.class )
					.setParameter( "id", id )
					.getSingleResult() );
		} );
	}

	@Entity(name = "LazyShipment")
	@Table(name = "lazy_soft_shipment")
	@SoftDelete
	static class LazyShipment {
		@Id
		Integer id;
		@PartitionKey
		@Basic(fetch = LAZY)
		@Column(nullable = false)
		String region;

		LazyShipment() {
		}

		LazyShipment(Integer id, String region) {
			this.id = id;
			this.region = region;
		}
	}

	@Entity(name = "LazyParcel")
	@Table(name = "lazy_soft_parcel")
	@SecondaryTable(name = "lazy_soft_parcel_details")
	@SoftDelete
	static class LazyParcel {
		@Id
		Integer id;
		@PartitionKey
		@Basic(fetch = LAZY)
		@Column(table = "lazy_soft_parcel_details", nullable = false)
		String region;

		LazyParcel() {
		}

		LazyParcel(Integer id, String region) {
			this.id = id;
			this.region = region;
		}
	}

	@Entity(name = "LazyCrate")
	@Table(name = "lazy_crate")
	static class LazyCrate {
		@Id
		Integer id;
		@PartitionKey
		@Basic(fetch = LAZY)
		@Column(nullable = false)
		String region;
		String label;

		LazyCrate() {
		}

		LazyCrate(Integer id, String region) {
			this.id = id;
			this.region = region;
		}
	}

	@Entity(name = "Pallet")
	@Table(name = "read_only_soft_pallet")
	@SoftDelete
	static class Pallet {
		@Id
		Integer id;
		@PartitionKey
		@Column(nullable = false, updatable = false)
		String region;

		Pallet() {
		}

		Pallet(Integer id, String region) {
			this.id = id;
			this.region = region;
		}
	}

	@Entity(name = "Container")
	@Table(name = "read_only_container")
	static class Container {
		@Id
		Integer id;
		@PartitionKey
		@Column(nullable = false, updatable = false)
		String region;

		Container() {
		}

		Container(Integer id, String region) {
			this.id = id;
			this.region = region;
		}
	}

	@Entity(name = "CustomDeleteContainer")
	@Table(name = "custom_delete_container")
	@SQLDelete(sql = "delete from custom_delete_container where id = ? and region = ?")
	static class CustomDeleteContainer {
		@Id
		Integer id;
		@PartitionKey
		@Basic(fetch = LAZY)
		@Column(nullable = false, updatable = false)
		String region;

		CustomDeleteContainer() {
		}

		CustomDeleteContainer(Integer id, String region) {
			this.id = id;
			this.region = region;
		}
	}
}
