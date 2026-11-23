package org.hibernate.orm.test.boot.models.xml.attr;

import java.util.Set;

import org.hibernate.annotations.Temporal;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

@JiraKey("HHH-20617")
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"))
public class TemporalVersioningTests {

	@Test
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testTemporalVersioningFromXml(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/attr/temporal/temporal-versioning.xml" )
				.build();

		final ModelsContext modelsContext = createBuildingContext( managedResources, serviceRegistry );
		final ClassDetailsRegistry classDetailsRegistry = modelsContext.getClassDetailsRegistry();

		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( SimpleEntity.class.getName() );

		// entity-level @Temporal
		final Temporal temporal = classDetails.getDirectAnnotationUsage( Temporal.class );
		assertThat( temporal )
				.as( "Entity with <temporal-versioning> should have @Temporal" )
				.isNotNull();
		assertThat( temporal.rowStart() ).isEqualTo( "eff_from" );
		assertThat( temporal.rowEnd() ).isEqualTo( "eff_to" );
		assertThat( temporal.secondPrecision() ).isEqualTo( 6 );

		// nested @Temporal.HistoryPartitioning
		final Temporal.HistoryPartitioning partitioning =
				classDetails.getDirectAnnotationUsage( Temporal.HistoryPartitioning.class );
		assertThat( partitioning )
				.as( "Nested <history-partitioning> should produce @Temporal.HistoryPartitioning" )
				.isNotNull();
		assertThat( partitioning.currentPartition() ).isEqualTo( "cur" );
		assertThat( partitioning.historyPartition() ).isEqualTo( "hist" );

		// nested @Temporal.HistoryTable
		final Temporal.HistoryTable historyTable =
				classDetails.getDirectAnnotationUsage( Temporal.HistoryTable.class );
		assertThat( historyTable )
				.as( "Nested <history-table> should produce @Temporal.HistoryTable" )
				.isNotNull();
		assertThat( historyTable.name() ).isEqualTo( "SIMPLE_HISTORY" );

		// collection-level @Temporal
		final FieldDetails stringsField = classDetails.findFieldByName( "strings" );
		assertThat( stringsField.getDirectAnnotationUsage( Temporal.class ) )
				.as( "Collection with <temporal-versioning> should have @Temporal" )
				.isNotNull();

		// @Temporal.Excluded via <exclude-from-temporal-versioning>
		final FieldDetails excludedField = classDetails.findFieldByName( "excluded" );
		assertThat( excludedField.getDirectAnnotationUsage( Temporal.Excluded.class ) )
				.as( "Basic with <exclude-from-temporal-versioning> should have @Temporal.Excluded" )
				.isNotNull();

		final FieldDetails idField = classDetails.findFieldByName( "id" );
		assertThat( idField.getDirectAnnotationUsage( Temporal.Excluded.class ) )
				.as( "Attribute without <exclude-from-temporal-versioning> should not have @Temporal.Excluded" )
				.isNull();
	}

	@SuppressWarnings("unused")
	public static class SimpleEntity {
		private Integer id;
		private Set<String> strings;
		private long excluded;
	}
}
