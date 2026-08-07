/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.attr.transientattr;

import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.ModelsContext;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Transient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

@SuppressWarnings("JUnitMalformedDeclaration")
public class EmbeddableTransientTests {

	private static final String MAPPING_XML =
			"mappings/models/attr/transientattr/mapping.xml";

	@ServiceRegistry
	@Test
	void testSourceModel(ServiceRegistryScope scope) {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( MAPPING_XML )
				.build();
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final ModelsContext modelsContext = createBuildingContext( managedResources, serviceRegistry );

		// entity uses access="FIELD", so @Transient is applied to the field
		final ClassDetails entityDetails = modelsContext.getClassDetailsRegistry()
				.getClassDetails( EntityWithEmbeddable.class.getName() );

		final FieldDetails nameField = entityDetails.findFieldByName( "name" );
		assertThat( nameField.getDirectAnnotationUsage( Transient.class ) ).isNull();

		final FieldDetails displayLabelField = entityDetails.findFieldByName( "displayLabel" );
		assertThat( displayLabelField.getDirectAnnotationUsage( Transient.class ) ).isNotNull();

		// embeddable uses access="PROPERTY", so @Transient is applied to getters
		final ClassDetails embeddableDetails = modelsContext.getClassDetailsRegistry()
				.getClassDetails( EmbeddableWithTransient.class.getName() );

		final MemberDetails streetGetter = findGetter( embeddableDetails, "getStreet" );
		assertThat( streetGetter ).isNotNull();
		assertThat( streetGetter.getDirectAnnotationUsage( Transient.class ) ).isNull();

		final MemberDetails fullAddressGetter = findGetter( embeddableDetails, "getFullAddress" );
		assertThat( fullAddressGetter ).isNotNull();
		assertThat( fullAddressGetter.getDirectAnnotationUsage( Transient.class ) ).isNotNull();

		final MemberDetails componentGetter = findGetter( embeddableDetails, "getComponent" );
		assertThat( componentGetter ).isNotNull();
		assertThat( componentGetter.getDirectAnnotationUsage( Transient.class ) ).isNotNull();
	}

	private static MemberDetails findGetter(ClassDetails classDetails, String getterName) {
		for ( int i = 0; i < classDetails.getMethods().size(); i++ ) {
			final MethodDetails method = classDetails.getMethods().get( i );
			if ( method.getName().equals( getterName ) ) {
				return method;
			}
		}
		return null;
	}

	@ServiceRegistry
	@DomainModel(xmlMappings = MAPPING_XML)
	@Test
	void testMappingModel(DomainModelScope domainModelScope) {
		domainModelScope.withHierarchy( EntityWithEmbeddable.class, (rootClass) -> {
			// entity: displayLabel is transient, name is persistent
			assertThat( rootClass.getProperties().stream().map( Property::getName ) )
					.contains( "name", "address" );

			// embeddable: fullAddress and component are transient
			final Property addressProperty = rootClass.getProperty( "address" );
			final Component comp = (Component) addressProperty.getValue();

			assertThat( comp.getProperty( "street" ) ).isNotNull();
			assertThat( comp.getProperty( "city" ) ).isNotNull();

			assertThat( comp.getProperties().stream().map( Property::getName ) )
					.containsExactlyInAnyOrder( "street", "city" );
		} );
	}


}
