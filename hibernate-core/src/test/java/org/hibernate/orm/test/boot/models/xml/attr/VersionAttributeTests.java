package org.hibernate.orm.test.boot.models.xml.attr;

import java.sql.Timestamp;

import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

@ServiceRegistry
public class VersionAttributeTests {

	@Test
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testUpdateTimestampSource(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/attr/version/update-timestamp.xml" )
				.build();

		final ModelsContext modelsContext = createBuildingContext( managedResources, serviceRegistry );
		final ClassDetailsRegistry classDetailsRegistry = modelsContext.getClassDetailsRegistry();

		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( SimpleEntity.class.getName() );

		final FieldDetails versionField = classDetails.findFieldByName( "version" );
		final UpdateTimestamp updateTimestamp = versionField.getDirectAnnotationUsage( UpdateTimestamp.class );
		assertThat( updateTimestamp )
				.as( "<version> with <update-timestamp source=\"DB\"/> should have @UpdateTimestamp" )
				.isNotNull();
		assertThat( updateTimestamp.source() ).isEqualTo( SourceType.DB );
	}

	@SuppressWarnings("unused")
	@Entity(name = "SimpleEntity")
	@Table(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		private Integer id;
		@Version
		private Timestamp version;
		private String name;
	}
}
