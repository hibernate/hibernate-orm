package org.hibernate.orm.test.mapping.identifier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.PartitionKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		NullablePartitionKeyMutationTest.Crate.class,
		NullablePartitionKeyMutationTest.DynamicCrate.class,
		NullablePartitionKeyMutationTest.SecondaryCrate.class,
		NullablePartitionKeyMutationTest.VersionedCrate.class
})
@SessionFactory(useCollectingStatementObserver = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNullablePartitionKey.class)
@JiraKey("HHH-21272")
public class NullablePartitionKeyMutationTest {
	@Test
	void updateAndDeleteWithNullPartitionKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Crate( 1 ) );
			session.persist( new DynamicCrate( 1 ) );
		} );
		final var statements = scope.getCollectingStatementObserver();
		statements.clear();
		scope.inTransaction( session -> {
			final var crate = session.find( Crate.class, 1 );
			crate.label = "updated";
			crate.region = "eu";
			session.find( DynamicCrate.class, 1 ).label = "updated";
		} );
		assertThat( statements.getSqlQueries() )
				.anyMatch( sql -> sql.startsWith( "update partition_crate " ) && sql.contains( "region is null" ) )
				.anyMatch( sql -> sql.startsWith( "update partition_dynamic_crate " ) && sql.contains( "region is null" ) );
		scope.inTransaction( session -> {
			final var crate = session.find( Crate.class, 1 );
			assertThat( crate.label ).isEqualTo( "updated" );
			assertThat( crate.region ).isEqualTo( "eu" );
			session.remove( crate );
			final var dynamicCrate = session.find( DynamicCrate.class, 1 );
			assertThat( dynamicCrate.label ).isEqualTo( "updated" );
			session.remove( dynamicCrate );
		} );
		assertThat( statements.getSqlQueries() )
				.anyMatch( sql -> sql.startsWith( "delete from partition_dynamic_crate " )
						&& sql.contains( "region is null" ) );
		scope.inTransaction( session -> {
			assertThat( session.find( Crate.class, 1 ) ).isNull();
			assertThat( session.find( DynamicCrate.class, 1 ) ).isNull();
		} );
	}

	@Test
	void updateOptionalTableWithNullPartitionKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new SecondaryCrate( 1 ) ) );
		final var statements = scope.getCollectingStatementObserver();
		statements.clear();
		scope.inTransaction( session -> session.find( SecondaryCrate.class, 1 ).label = "updated" );
		assertThat( statements.getSqlQueries() ).anyMatch( sql ->
				sql.contains( "partition_crate_detail" ) && sql.contains( "region is null" ) );
		scope.inTransaction( session -> {
			final var crate = session.find( SecondaryCrate.class, 1 );
			assertThat( crate.label ).isEqualTo( "updated" );
			session.remove( crate );
		} );
	}

	/**
	 * A delete with a null partition key is not added to the batch of a delete with a non-null key.
	 */
	@Test
	@JiraKey("HHH-21279")
	void removeBatchWithNullAndNonNullPartitionKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var crate = new Crate( 1 );
			crate.region = "eu";
			session.persist( crate );
			session.persist( new Crate( 2 ) );
		} );
		scope.inTransaction( session -> {
			session.setJdbcBatchSize( 5 );
			session.remove( session.find( Crate.class, 1 ) );
			session.remove( session.find( Crate.class, 2 ) );
		} );
		scope.inTransaction( session -> {
			assertThat( session.find( Crate.class, 1 ) ).isNull();
			assertThat( session.find( Crate.class, 2 ) ).isNull();
		} );
	}

	@Test
	@JiraKey("HHH-21279")
	void forceVersionIncrementWithNullPartitionKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new VersionedCrate( 1 ) ) );
		final var statements = scope.getCollectingStatementObserver();
		statements.clear();
		scope.inTransaction( session ->
				session.find( VersionedCrate.class, 1, LockModeType.OPTIMISTIC_FORCE_INCREMENT ) );
		assertThat( statements.getSqlQueries() ).last().asString()
				.startsWith( "update partition_versioned_crate " ).contains( "region is null" );
		scope.inTransaction( session -> {
			final var crate = session.find( VersionedCrate.class, 1 );
			assertThat( crate.version ).isEqualTo( 1 );
			session.remove( crate );
		} );
	}

	@Test
	@JiraKey("HHH-21279")
	void updateAndDeleteWithNullPartitionKeyInStatelessSession(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> session.insert( new Crate( 1 ) ) );
		final var statements = scope.getCollectingStatementObserver();
		statements.clear();
		scope.inStatelessTransaction( session -> {
			final var crate = session.get( Crate.class, 1 );
			crate.label = "updated";
			session.update( crate );
		} );
		assertThat( statements.getSqlQueries() ).last().asString()
				.startsWith( "update partition_crate " ).contains( "region is null" );
		scope.inStatelessTransaction( session -> {
			final var crate = session.get( Crate.class, 1 );
			assertThat( crate.label ).isEqualTo( "updated" );
			session.delete( crate );
		} );
		assertThat( statements.getSqlQueries() ).last().asString()
				.startsWith( "delete from partition_crate " ).contains( "region is null" );
		scope.inTransaction( session -> assertThat( session.find( Crate.class, 1 ) ).isNull() );
	}

	@Entity(name = "Crate")
	@Table(name = "partition_crate")
	public static class Crate {
		@Id
		Integer id;
		@PartitionKey
		String region;
		String label;

		Crate() {
		}

		Crate(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "DynamicCrate")
	@Table(name = "partition_dynamic_crate")
	@DynamicUpdate
	public static class DynamicCrate {
		@Id
		Integer id;
		@PartitionKey
		String region;
		String label;

		DynamicCrate() {
		}

		DynamicCrate(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "SecondaryCrate")
	@Table(name = "partition_secondary_crate")
	@SecondaryTable(name = "partition_crate_detail")
	public static class SecondaryCrate {
		@Id
		Integer id;
		@PartitionKey
		@Column(table = "partition_crate_detail")
		String region;
		@Column(table = "partition_crate_detail")
		String label;

		SecondaryCrate() {
		}

		SecondaryCrate(Integer id) {
			this.id = id;
			this.label = "original";
		}
	}

	@Entity(name = "VersionedCrate")
	@Table(name = "partition_versioned_crate")
	public static class VersionedCrate {
		@Id
		Integer id;
		@PartitionKey
		String region;
		@Version
		Integer version;

		VersionedCrate() {
		}

		VersionedCrate(Integer id) {
			this.id = id;
		}
	}
}
