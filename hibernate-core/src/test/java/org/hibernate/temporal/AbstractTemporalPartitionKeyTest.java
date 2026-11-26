package org.hibernate.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.PartitionKey;
import org.hibernate.annotations.Temporal;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.orm.junit.DialectContext.awaitTimestampTick;

/**
 * The update which ends the current revision of a temporal entity with a
 * partition key is restricted by the partition key.
 */
@JiraKey("HHH-21282")
public abstract class AbstractTemporalPartitionKeyTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Parcel( 1, "eu" ) ) );
		awaitTimestampTick();
		scope.getCollectingStatementObserver().clear();
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void updateAndRemove(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.find( Parcel.class, 1 ).label = "updated" );
		assertRevisionEndedByPartitionKey( scope, "region=?" );
		awaitTimestampTick();
		scope.inTransaction( session -> {
			final var parcel = session.find( Parcel.class, 1 );
			assertThat( parcel.label ).isEqualTo( "updated" );
			session.remove( parcel );
		} );
		assertRevisionEndedByPartitionKey( scope, "region=?" );
		scope.inTransaction( session -> assertThat( session.find( Parcel.class, 1 ) ).isNull() );
	}

	@Test
	void updateAndDeleteInStatelessSession(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			final var parcel = session.get( Parcel.class, 1 );
			parcel.label = "updated";
			session.update( parcel );
		} );
		assertRevisionEndedByPartitionKey( scope, "region=?" );
		awaitTimestampTick();
		scope.inStatelessTransaction( session -> {
			final var parcel = session.get( Parcel.class, 1 );
			assertThat( parcel.label ).isEqualTo( "updated" );
			session.delete( parcel );
		} );
		assertRevisionEndedByPartitionKey( scope, "region=?" );
		scope.inTransaction( session -> assertThat( session.find( Parcel.class, 1 ) ).isNull() );
	}

	/**
	 * An attribute which is excluded from versioning is updated in the current revision.
	 */
	@Test
	void updateExcludedAttribute(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.find( Parcel.class, 1 ).note = "updated" );
		assertThat( scope.getCollectingStatementObserver().getSqlQueries() )
				.filteredOn( sql -> sql.startsWith( "update " ) )
				.isNotEmpty()
				.allMatch( sql -> sql.contains( "region=?" ) );
		scope.inTransaction( session -> assertThat( session.find( Parcel.class, 1 ).note ).isEqualTo( "updated" ) );
	}

	/**
	 * A read-only instance has no snapshot of its state, so its partition key is not known.
	 */
	@Test
	void removeReadOnlyInstance(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setDefaultReadOnly( true );
			final var parcel = session.find( Parcel.class, 1 );
			parcel.region = "uk";
			session.remove( parcel );
		} );
		scope.inTransaction( session -> assertThat( session.find( Parcel.class, 1 ) ).isNull() );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNullablePartitionKey.class)
	void updateAndRemoveWithNullPartitionKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Parcel( 2, null ) ) );
		awaitTimestampTick();
		scope.getCollectingStatementObserver().clear();
		scope.inTransaction( session -> session.find( Parcel.class, 2 ).label = "updated" );
		assertRevisionEndedByPartitionKey( scope, "region is null" );
		awaitTimestampTick();
		scope.inTransaction( session -> {
			final var parcel = session.find( Parcel.class, 2 );
			assertThat( parcel.label ).isEqualTo( "updated" );
			session.remove( parcel );
		} );
		assertRevisionEndedByPartitionKey( scope, "region is null" );
		scope.inTransaction( session -> assertThat( session.find( Parcel.class, 2 ) ).isNull() );
	}

	private static void assertRevisionEndedByPartitionKey(SessionFactoryScope scope, String restriction) {
		final var statements = scope.getCollectingStatementObserver();
		assertThat( statements.getSqlQueries() )
				.filteredOn( sql -> sql.startsWith( "update " ) && sql.contains( "superseded" ) )
				.isNotEmpty()
				.allMatch( sql -> sql.contains( restriction ) );
		statements.clear();
	}

	@Entity(name = "Parcel")
	@Table(name = "temporal_partition_parcel")
	@Temporal
	public static class Parcel {
		@Id
		Integer id;
		@PartitionKey
		String region;
		String label;
		@Temporal.Excluded
		String note;

		Parcel() {
		}

		Parcel(Integer id, String region) {
			this.id = id;
			this.region = region;
		}
	}
}
