package org.hibernate.orm.test.boot.models.xml.attr;

import java.util.Set;

import org.hibernate.annotations.Audited;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

@JiraKey("HHH-20617")
@ServiceRegistry
public class AuditedXmlAttributeTests {

	@Test
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testAuditedFromXml(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/attr/audited/audited.xml" )
				.build();

		final ModelsContext modelsContext = createBuildingContext( managedResources, serviceRegistry );
		final ClassDetailsRegistry classDetailsRegistry = modelsContext.getClassDetailsRegistry();

		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( SimpleEntity.class.getName() );

		// entity-level @Audited
		final Audited audited = classDetails.getDirectAnnotationUsage( Audited.class );
		assertThat( audited )
				.as( "Entity with <audited> should have @Audited" )
				.isNotNull();

		// nested @Audited.Table
		final Audited.Table auditedTable =
				classDetails.getDirectAnnotationUsage( Audited.Table.class );
		assertThat( auditedTable )
				.as( "Nested <audited-table> should produce @Audited.Table" )
				.isNotNull();
		assertThat( auditedTable.name() ).isEqualTo( "SIMPLE_AUD" );
		assertThat( auditedTable.changesetIdColumn() ).isEqualTo( "CS_ID" );
		assertThat( auditedTable.modificationTypeColumn() ).isEqualTo( "CS_TYPE" );
		assertThat( auditedTable.invalidatingChangesetIdColumn() ).isEqualTo( "CS_END" );

		// collection-level @Audited
		final FieldDetails stringsField = classDetails.findFieldByName( "strings" );
		assertThat( stringsField.getDirectAnnotationUsage( Audited.class ) )
				.as( "Collection with <audited> should have @Audited" )
				.isNotNull();

		// @Audited.Excluded via <exclude-from-auditing>
		final FieldDetails excludedField = classDetails.findFieldByName( "excluded" );
		assertThat( excludedField.getDirectAnnotationUsage( Audited.Excluded.class ) )
				.as( "Basic with <exclude-from-auditing> should have @Audited.Excluded" )
				.isNotNull();

		final FieldDetails idField = classDetails.findFieldByName( "id" );
		assertThat( idField.getDirectAnnotationUsage( Audited.Excluded.class ) )
				.as( "Attribute without <exclude-from-auditing> should not have @Audited.Excluded" )
				.isNull();
	}

	@SuppressWarnings("unused")
	@Entity(name = "SimpleEntity")
	@Table(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		private Integer id;
		private Set<String> strings;
		private long excluded;
	}
}
