package org.hibernate.orm.test.boot.models.xml.attr;

import java.util.Date;

import org.hibernate.annotations.Immutable;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

@ServiceRegistry
public class BasicAttributeTests {

	@Test
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testMutableFalseAppliesImmutable(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/attr/basic/mutable.xml" )
				.build();

		final ModelsContext modelsContext = createBuildingContext( managedResources, serviceRegistry );
		final ClassDetailsRegistry classDetailsRegistry = modelsContext.getClassDetailsRegistry();

		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( SimpleEntity.class.getName() );

		final FieldDetails immutableDateField = classDetails.findFieldByName( "immutableDate" );
		assertThat( immutableDateField.getDirectAnnotationUsage( Immutable.class ) )
				.as( "Basic property with <mutable>false</mutable> should have @Immutable" )
				.isNotNull();

		final FieldDetails mutableDateField = classDetails.findFieldByName( "mutableDate" );
		assertThat( mutableDateField.getDirectAnnotationUsage( Immutable.class ) )
				.as( "Basic property with <mutable>true</mutable> should not have @Immutable" )
				.isNull();

		final FieldDetails nameField = classDetails.findFieldByName( "name" );
		assertThat( nameField.getDirectAnnotationUsage( Immutable.class ) )
				.as( "Basic property without <mutable> should not have @Immutable" )
				.isNull();
	}

	@Test
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testUpdateTimestampSource(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/attr/basic/update-timestamp.xml" )
				.build();

		final ModelsContext modelsContext = createBuildingContext( managedResources, serviceRegistry );
		final ClassDetailsRegistry classDetailsRegistry = modelsContext.getClassDetailsRegistry();

		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( UpdateTimestampEntity.class.getName() );

		final FieldDetails lastModifiedField = classDetails.findFieldByName( "lastModified" );
		final UpdateTimestamp updateTimestamp = lastModifiedField.getDirectAnnotationUsage( UpdateTimestamp.class );
		assertThat( updateTimestamp )
				.as( "<basic> with <update-timestamp source=\"DB\"/> should have @UpdateTimestamp" )
				.isNotNull();
		assertThat( updateTimestamp.source() ).isEqualTo( SourceType.DB );
	}

	@SuppressWarnings("unused")
	@Entity(name = "SimpleEntity")
	@Table(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		private Integer id;
		private Date mutableDate;
		private Date immutableDate;
		private String name;
	}

	@SuppressWarnings("unused")
	@Entity(name = "UpdateTimestampEntity")
	@Table(name = "UpdateTimestampEntity")
	public static class UpdateTimestampEntity {
		@Id
		private Integer id;
		private Date lastModified;
		private String name;
	}
}
