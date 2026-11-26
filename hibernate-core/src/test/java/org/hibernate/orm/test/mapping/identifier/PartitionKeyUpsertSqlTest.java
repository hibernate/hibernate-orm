package org.hibernate.orm.test.mapping.identifier;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.mapping.identifier.AbstractPartitionKeyMutationTest.Box;
import org.hibernate.orm.test.mapping.identifier.AbstractPartitionKeyMutationTest.Parrot;
import org.hibernate.orm.test.mapping.identifier.AbstractPartitionKeyMutationTest.Pet;
import org.hibernate.orm.test.mapping.identifier.AbstractPartitionKeyMutationTest.SealedCarton;
import org.hibernate.orm.test.mapping.identifier.AbstractPartitionKeyMutationTest.Truck;
import org.hibernate.orm.test.mapping.identifier.AbstractPartitionKeyMutationTest.Vehicle;
import org.hibernate.sql.spi.mutation.jdbc.PreparableMutationOperation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.JdbcSettings.ALLOW_METADATA_ON_BOOT;
import static org.hibernate.cfg.JdbcSettings.DIALECT;

/**
 * An upsert matches the row by its primary key. The partition key belongs to
 * the primary key on some databases, and is then written once, as part of the
 * key. On other databases it is a column like any other. The statements are
 * only generated, and never executed.
 */
@DomainModel(annotatedClasses = { Box.class, Vehicle.class, Truck.class, Pet.class, Parrot.class, SealedCarton.class })
@RequiresDialect(H2Dialect.class)
@JiraKey("HHH-21281")
public class PartitionKeyUpsertSqlTest {
	@Test
	@ServiceRegistry(settings = {
			@Setting(name = DIALECT, value = "org.hibernate.dialect.PostgreSQLDialect"),
			@Setting(name = ALLOW_METADATA_ON_BOOT, value = "false")
	})
	@SessionFactory(exportSchema = false)
	void conflictTargetIncludesPartitionKeyOfPrimaryKey(SessionFactoryScope scope) {
		assertThat( upsertStatements( scope, Box.class ) ).containsExactly(
				"insert into partition_box (label,id,region) values (?,?,?)"
						+ " on conflict (id,region) do update set label=excluded.label"
		);
	}

	/**
	 * The primary key of the table of a joined subclass includes its partition key.
	 */
	@Test
	@ServiceRegistry(settings = {
			@Setting(name = DIALECT, value = "org.hibernate.dialect.PostgreSQLDialect"),
			@Setting(name = ALLOW_METADATA_ON_BOOT, value = "false")
	})
	@SessionFactory(exportSchema = false)
	void conflictTargetIncludesPartitionKeyOfJoinedSubclass(SessionFactoryScope scope) {
		assertThat( upsertStatements( scope, Truck.class ) ).containsExactly(
				"insert into partition_vehicle (name,id) values (?,?)"
						+ " on conflict (id) do update set name=excluded.name",
				"insert into partition_truck (payload,id,region) values (?,?,?)"
						+ " on conflict (id,region) do update set payload=excluded.payload"
		);
	}

	/**
	 * The primary key of a table shared with a superclass does not include
	 * the partition key of a subclass.
	 */
	@Test
	@ServiceRegistry(settings = {
			@Setting(name = DIALECT, value = "org.hibernate.dialect.PostgreSQLDialect"),
			@Setting(name = ALLOW_METADATA_ON_BOOT, value = "false")
	})
	@SessionFactory(exportSchema = false)
	void conflictTargetExcludesPartitionKeyOfSingleTableSubclass(SessionFactoryScope scope) {
		assertThat( scope.getMetadataImplementor().getEntityBinding( Pet.class.getName() ).getTable()
				.getPrimaryKey().getColumns() )
				.extracting( org.hibernate.mapping.Column::getName )
				.containsExactly( "id" );
		assertThat( upsertStatements( scope, Parrot.class ) ).singleElement().asString()
				.startsWith( "insert into partition_pet " )
				.contains( " on conflict (id) do update set " )
				.contains( "region=excluded.region" );
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(name = DIALECT, value = "org.hibernate.dialect.SpannerPostgreSQLDialect"),
			@Setting(name = ALLOW_METADATA_ON_BOOT, value = "false")
	})
	@SessionFactory(exportSchema = false)
	void conflictTargetIsPrimaryKeyWithoutPartitionKey(SessionFactoryScope scope) {
		assertThat( upsertStatements( scope, Box.class ) ).containsExactly(
				"insert into partition_box (label,region,id) values (?,?,?)"
						+ " on conflict (id) do update set label=excluded.label,region=excluded.region"
		);
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(name = DIALECT, value = "org.hibernate.dialect.SQLServerDialect"),
			@Setting(name = ALLOW_METADATA_ON_BOOT, value = "false")
	})
	@SessionFactory(exportSchema = false)
	void mergeMatchesPartitionKeyOfPrimaryKey(SessionFactoryScope scope) {
		assertThat( upsertStatements( scope, Box.class ) ).singleElement().asString()
				.contains( "using (select cast(? as int) id,cast(? as varchar(255)) region,cast(? as varchar(255)) label)" )
				.contains( " on (t.id=s.id and t.region=s.region) " )
				.contains( " insert (id,region,label) " )
				.contains( " update set label=s.label" );
		assertMergeChecksStoredPartitionKey( scope );
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(name = DIALECT, value = "org.hibernate.dialect.DB2Dialect"),
			@Setting(name = ALLOW_METADATA_ON_BOOT, value = "false")
	})
	@SessionFactory(exportSchema = false)
	void db2MergeChecksStoredPartitionKey(SessionFactoryScope scope) {
		assertMergeChecksStoredPartitionKey( scope );
	}

	private static void assertMergeChecksStoredPartitionKey(SessionFactoryScope scope) {
		assertThat( upsertStatements( scope, SealedCarton.class ) )
				.filteredOn( sql -> sql.startsWith( "merge into partition_sealed_carton_detail " ) )
				.singleElement().asString()
				.contains( "when matched and s.label is null and t.region is null then delete" )
				.contains( "then update set label=s.label" );
	}

	@Test
	@SessionFactory(exportSchema = false)
	void mergeMatchesPrimaryKeyWithoutPartitionKey(SessionFactoryScope scope) {
		assertThat( upsertStatements( scope, Box.class ) ).singleElement().asString()
				.contains( " on (t.id=s.id) " )
				.contains( " insert (id,label,region) " )
				.contains( " update set label=s.label,region=s.region" );
	}

	private static List<String> upsertStatements(SessionFactoryScope scope, Class<?> entityClass) {
		final var operations = scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( entityClass )
				.getMergeCoordinator()
				.getStaticMutationOperationGroup();
		final var statements = new ArrayList<String>();
		operations.forEachOperation( (position, operation) ->
				statements.add( ( (PreparableMutationOperation) operation ).getSqlString() ) );
		return statements;
	}
}
