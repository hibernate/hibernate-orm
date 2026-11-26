package org.hibernate.orm.test.mapping.identifier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.PartitionKey;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An update or delete of an entity with a partition key is restricted by the
 * partition key, in the table which has the partition key column.
 */
public abstract class AbstractPartitionKeyMutationTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Box( 1, "eu" ) );
			session.persist( new VersionedBox( 1, "eu" ) );
			session.persist( new CustomDeleteBox( 1, "eu" ) );
			session.persist( new Truck( 1, "eu" ) );
		} );
		scope.getCollectingStatementObserver().clear();
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	/**
	 * An unloaded reference is loaded before it is removed, since its partition key is not known.
	 */
	@Test
	void removeUnloadedReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.remove( session.getReference( Box.class, 1 ) ) );
		assertThat( scope.getCollectingStatementObserver().getSqlQueries() ).last().asString()
				.startsWith( "delete from partition_box " ).contains( "region=?" );
		scope.inTransaction( session -> assertThat( session.find( Box.class, 1 ) ).isNull() );
	}

	@Test
	void removeUnloadedVersionedReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.remove( session.getReference( VersionedBox.class, 1 ) ) );
		assertThat( scope.getCollectingStatementObserver().getSqlQueries() ).last().asString()
				.startsWith( "delete from partition_versioned_box " ).contains( "region=?", "version=?" );
		scope.inTransaction( session -> assertThat( session.find( VersionedBox.class, 1 ) ).isNull() );
	}

	@Test
	void removeUnloadedReferenceWithCustomDelete(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.remove( session.getReference( CustomDeleteBox.class, 1 ) ) );
		scope.inTransaction( session -> assertThat( session.find( CustomDeleteBox.class, 1 ) ).isNull() );
	}

	/**
	 * A read-only instance has no snapshot of its state, so its partition key is not known.
	 */
	@Test
	void removeReadOnlyInstance(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setDefaultReadOnly( true );
			final var box = session.find( VersionedBox.class, 1 );
			box.region = "uk";
			session.remove( box );
		} );
		scope.inTransaction( session -> assertThat( session.find( VersionedBox.class, 1 ) ).isNull() );
	}

	@Test
	void forceVersionIncrementOfReadOnlyInstance(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setDefaultReadOnly( true );
			session.find( VersionedBox.class, 1, LockModeType.OPTIMISTIC_FORCE_INCREMENT );
		} );
		scope.inTransaction( session -> assertThat( session.find( VersionedBox.class, 1 ).version ).isEqualTo( 1 ) );
	}

	@Test
	void updateAndRemoveJoinedSubclassWithPartitionKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var truck = session.find( Truck.class, 1 );
			truck.name = "updated";
			truck.payload = "updated";
		} );
		assertRestrictedByPartitionKey( scope, "partition_truck", "partition_vehicle" );
		scope.inTransaction( session -> {
			final var truck = session.find( Truck.class, 1 );
			assertThat( truck.name ).isEqualTo( "updated" );
			assertThat( truck.payload ).isEqualTo( "updated" );
			session.remove( truck );
		} );
		assertRestrictedByPartitionKey( scope, "partition_truck", "partition_vehicle" );
		scope.inTransaction( session -> assertThat( session.find( Truck.class, 1 ) ).isNull() );
	}

	/**
	 * A stateless session has no snapshot of the state of an entity, so a delete
	 * is restricted by the partition key of the given instance.
	 */
	@Test
	@JiraKey("HHH-21279")
	void deleteInStatelessSession(SessionFactoryScope scope) {
		final var statements = scope.getCollectingStatementObserver();
		scope.inStatelessTransaction( session -> {
			session.delete( session.get( Box.class, 1 ) );
			session.delete( session.get( VersionedBox.class, 1 ) );
			session.delete( session.get( CustomDeleteBox.class, 1 ) );
		} );
		assertThat( statements.getSqlQueries() )
				.anyMatch( sql -> sql.startsWith( "delete from partition_box " ) && sql.contains( "region=?" ) )
				.anyMatch( sql -> sql.startsWith( "delete from partition_versioned_box " )
						&& sql.contains( "region=?" ) && sql.contains( "version=?" ) );
		scope.inTransaction( session -> {
			assertThat( session.find( Box.class, 1 ) ).isNull();
			assertThat( session.find( VersionedBox.class, 1 ) ).isNull();
			assertThat( session.find( CustomDeleteBox.class, 1 ) ).isNull();
		} );
	}

	@Test
	@JiraKey("HHH-21279")
	void updateAndDeleteJoinedSubclassWithPartitionKeyInStatelessSession(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			final var truck = session.get( Truck.class, 1 );
			truck.name = "updated";
			truck.payload = "updated";
			session.update( truck );
		} );
		assertRestrictedByPartitionKey( scope, "partition_truck", "partition_vehicle" );
		scope.inStatelessTransaction( session -> {
			final var truck = session.get( Truck.class, 1 );
			assertThat( truck.name ).isEqualTo( "updated" );
			assertThat( truck.payload ).isEqualTo( "updated" );
			session.delete( truck );
		} );
		assertRestrictedByPartitionKey( scope, "partition_truck", "partition_vehicle" );
		scope.inTransaction( session -> assertThat( session.find( Truck.class, 1 ) ).isNull() );
	}

	/**
	 * Only the statements for the table with the partition key column are restricted by it.
	 */
	static void assertRestrictedByPartitionKey(SessionFactoryScope scope, String partitionedTable, String otherTable) {
		final var statements = scope.getCollectingStatementObserver();
		assertThat( statements.getSqlQueries() )
				.filteredOn( sql -> isMutationOf( sql, partitionedTable ) )
				.isNotEmpty()
				.allMatch( sql -> sql.contains( "region=?" ) );
		assertThat( statements.getSqlQueries() )
				.filteredOn( sql -> isMutationOf( sql, otherTable ) )
				.isNotEmpty()
				.noneMatch( sql -> sql.contains( "region" ) );
		statements.clear();
	}

	private static boolean isMutationOf(String sql, String table) {
		return sql.startsWith( "update " + table + " " )
			|| sql.startsWith( "delete from " + table + " " );
	}

	@Entity(name = "Box")
	@Table(name = "partition_box")
	public static class Box {
		@Id
		Integer id;
		@PartitionKey
		@Column(nullable = false)
		String region;
		String label;

		Box() {
		}

		Box(Integer id, String region) {
			this.id = id;
			this.region = region;
		}
	}

	@Entity(name = "VersionedBox")
	@Table(name = "partition_versioned_box")
	public static class VersionedBox {
		@Id
		Integer id;
		@PartitionKey
		@Column(nullable = false, updatable = false)
		String region;
		@Version
		Integer version;

		VersionedBox() {
		}

		VersionedBox(Integer id, String region) {
			this.id = id;
			this.region = region;
		}
	}

	@Entity(name = "CustomDeleteBox")
	@Table(name = "partition_custom_delete_box")
	@SQLDelete(sql = "delete from partition_custom_delete_box where id = ? and region = ?")
	public static class CustomDeleteBox {
		@Id
		Integer id;
		@PartitionKey
		@Column(nullable = false)
		String region;

		CustomDeleteBox() {
		}

		CustomDeleteBox(Integer id, String region) {
			this.id = id;
			this.region = region;
		}
	}

	@Entity(name = "Vehicle")
	@Table(name = "partition_vehicle")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Vehicle {
		@Id
		Integer id;
		String name;

		Vehicle() {
		}

		Vehicle(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Truck")
	@Table(name = "partition_truck")
	public static class Truck extends Vehicle {
		@PartitionKey
		@Column(nullable = false)
		String region;
		String payload;

		Truck() {
		}

		Truck(Integer id, String region) {
			super( id );
			this.region = region;
		}
	}
}
