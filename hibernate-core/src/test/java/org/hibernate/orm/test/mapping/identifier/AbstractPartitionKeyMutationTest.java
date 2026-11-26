package org.hibernate.orm.test.mapping.identifier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.PartitionKey;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
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
			session.persist( new Carton( 1, "eu" ) );
			session.persist( new SealedCarton( 1, "eu" ) );
			session.persist( new Tag( 1, "eu" ) );
			session.persist( new Parrot( 1, "eu" ) );
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
	 * The row of an optional table which has the partition key column has a value
	 * in that column, so it exists, and is updated rather than merged.
	 */
	@Test
	@JiraKey("HHH-21281")
	void updateOptionalTableWithPartitionKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.find( Carton.class, 1 ).label = "updated" );
		assertThat( scope.getCollectingStatementObserver().getSqlQueries() )
				.anyMatch( sql -> sql.startsWith( "update partition_carton_detail " ) && sql.contains( "region=?" ) );
		scope.inTransaction( session -> {
			final var carton = session.find( Carton.class, 1 );
			assertThat( carton.label ).isEqualTo( "updated" );
			carton.label = null;
		} );
		scope.inTransaction( session -> {
			final var carton = session.find( Carton.class, 1 );
			assertThat( carton.label ).isNull();
			assertThat( carton.region ).isEqualTo( "eu" );
			session.remove( carton );
		} );
		scope.inTransaction( session -> assertThat( session.find( Carton.class, 1 ) ).isNull() );
	}

	/**
	 * The row of an optional table is not deleted when its other columns are
	 * null, since it has a value in its partition key column.
	 */
	@Test
	@JiraKey("HHH-21283")
	void clearOptionalTableWithNonUpdatablePartitionKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.find( SealedCarton.class, 1 ).label = null );
		scope.inTransaction( session -> {
			final var carton = session.find( SealedCarton.class, 1 );
			assertThat( carton.label ).isNull();
			assertThat( carton.region ).isEqualTo( "eu" );
			carton.label = "restored";
		} );
		scope.inStatelessTransaction( session -> {
			final var carton = session.get( SealedCarton.class, 1 );
			assertThat( carton.label ).isEqualTo( "restored" );
			carton.label = null;
			session.update( carton );
		} );
		scope.inTransaction( session -> {
			final var carton = session.find( SealedCarton.class, 1 );
			assertThat( carton.label ).isNull();
			assertThat( carton.region ).isEqualTo( "eu" );
		} );
	}

	@Test
	@JiraKey("HHH-21281")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsPrimaryKeyUpdate.class)
	void changePartitionKeyInOptionalTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var carton = session.find( Carton.class, 1 );
			carton.region = "uk";
			carton.label = "updated";
		} );
		scope.inTransaction( session -> {
			final var carton = session.find( Carton.class, 1 );
			assertThat( carton.region ).isEqualTo( "uk" );
			assertThat( carton.label ).isEqualTo( "updated" );
		} );
	}

	/**
	 * An upsert matches the row by its partition key, and does not write the column twice.
	 */
	@Test
	@JiraKey("HHH-21281")
	void upsertInStatelessSession(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			final var existing = new Box( 1, "eu" );
			existing.label = "updated";
			session.upsert( existing );
			final var inserted = new Box( 2, "eu" );
			inserted.label = "inserted";
			session.upsert( inserted );
		} );
		scope.inTransaction( session -> {
			final var existing = session.find( Box.class, 1 );
			assertThat( existing.label ).isEqualTo( "updated" );
			assertThat( existing.region ).isEqualTo( "eu" );
			final var inserted = session.find( Box.class, 2 );
			assertThat( inserted.label ).isEqualTo( "inserted" );
			assertThat( inserted.region ).isEqualTo( "eu" );
		} );
	}

	@Test
	@JiraKey("HHH-21281")
	void upsertOptionalTableInStatelessSession(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			final var existing = new Carton( 1, "eu" );
			existing.label = "updated";
			session.upsert( existing );
			// a row with a partition key is not deleted when its other columns are null
			session.upsert( new Carton( 1, "eu" ) );
			final var inserted = new Carton( 2, "eu" );
			inserted.label = "inserted";
			session.upsert( inserted );
		} );
		scope.inTransaction( session -> {
			final var existing = session.find( Carton.class, 1 );
			assertThat( existing.label ).isNull();
			assertThat( existing.region ).isEqualTo( "eu" );
			final var inserted = session.find( Carton.class, 2 );
			assertThat( inserted.label ).isEqualTo( "inserted" );
			assertThat( inserted.region ).isEqualTo( "eu" );
		} );
	}

	/**
	 * An upsert into an optional table whose only column is the partition key
	 * has nothing to assign when it matches the row by the partition key.
	 */
	@Test
	@JiraKey("HHH-21281")
	void upsertOptionalTableWithOnlyPartitionKeyInStatelessSession(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			session.upsert( new Tag( 1, "eu" ) );
			session.upsert( new Tag( 2, "eu" ) );
		} );
		scope.inTransaction( session -> {
			assertThat( session.find( Tag.class, 1 ).region ).isEqualTo( "eu" );
			assertThat( session.find( Tag.class, 2 ).region ).isEqualTo( "eu" );
		} );
	}

	/**
	 * The partition key of a subclass which shares the table of its superclass
	 * does not belong to the primary key of the table, even where a partition
	 * key usually does, so an upsert does not match the row by it.
	 */
	@Test
	@JiraKey("HHH-21281")
	void upsertSubclassWithPartitionKeyInStatelessSession(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			final var existing = new Parrot( 1, "eu" );
			existing.name = "updated";
			session.upsert( existing );
			final var inserted = new Parrot( 2, "eu" );
			inserted.name = "inserted";
			session.upsert( inserted );
		} );
		scope.inTransaction( session -> {
			final var existing = session.find( Parrot.class, 1 );
			assertThat( existing.name ).isEqualTo( "updated" );
			assertThat( existing.region ).isEqualTo( "eu" );
			final var inserted = session.find( Parrot.class, 2 );
			assertThat( inserted.name ).isEqualTo( "inserted" );
			assertThat( inserted.region ).isEqualTo( "eu" );
		} );
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

	@Entity(name = "Carton")
	@Table(name = "partition_carton")
	@SecondaryTable(name = "partition_carton_detail")
	public static class Carton {
		@Id
		Integer id;
		@PartitionKey
		@Column(table = "partition_carton_detail", nullable = false)
		String region;
		@Column(table = "partition_carton_detail")
		String label;

		Carton() {
		}

		Carton(Integer id, String region) {
			this.id = id;
			this.region = region;
		}
	}

	@Entity(name = "SealedCarton")
	@Table(name = "partition_sealed_carton")
	@SecondaryTable(name = "partition_sealed_carton_detail")
	public static class SealedCarton {
		@Id
		Integer id;
		@PartitionKey
		@Column(table = "partition_sealed_carton_detail", nullable = false, updatable = false)
		String region;
		@Column(table = "partition_sealed_carton_detail")
		String label;

		SealedCarton() {
		}

		SealedCarton(Integer id, String region) {
			this.id = id;
			this.region = region;
			this.label = "label";
		}
	}

	@Entity(name = "Tag")
	@Table(name = "partition_tag")
	@SecondaryTable(name = "partition_tag_detail")
	public static class Tag {
		@Id
		Integer id;
		@PartitionKey
		@Column(table = "partition_tag_detail", nullable = false)
		String region;

		Tag() {
		}

		Tag(Integer id, String region) {
			this.id = id;
			this.region = region;
		}
	}

	@Entity(name = "Pet")
	@Table(name = "partition_pet")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class Pet {
		@Id
		Integer id;
		String name;

		Pet() {
		}

		Pet(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Parrot")
	public static class Parrot extends Pet {
		@PartitionKey
		String region;

		Parrot() {
		}

		Parrot(Integer id, String region) {
			super( id );
			this.region = region;
		}
	}
}
