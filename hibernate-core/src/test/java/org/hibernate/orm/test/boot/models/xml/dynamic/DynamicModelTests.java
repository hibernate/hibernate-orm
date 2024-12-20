/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.dynamic;

import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SortNatural;
import org.hibernate.boot.internal.CollectionClassification;
import org.hibernate.boot.internal.LimitedCollectionClassification;
import org.hibernate.boot.internal.Target;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class DynamicModelTests {
	@Test
	@ServiceRegistry
	void testSimpleDynamicModel(ServiceRegistryScope registryScope) {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/dynamic/dynamic-simple.xml" )
				.build();
		final SourceModelBuildingContext sourceModelBuildingContext = createBuildingContext( managedResources, registryScope.getRegistry() );

		final ClassDetailsRegistry classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();

		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( "SimpleEntity" );
		assertThat( classDetails.getClassName() ).isNull();
		assertThat( classDetails.getName() ).isEqualTo( "SimpleEntity" );

		final FieldDetails idField = classDetails.findFieldByName( "id" );
		assertThat( idField.getType().determineRawClass().getClassName() ).isEqualTo( Integer.class.getName() );

		final FieldDetails nameField = classDetails.findFieldByName( "name" );
		assertThat( nameField.getType().determineRawClass().getClassName() ).isEqualTo( String.class.getName() );
		assertThat( nameField.getDirectAnnotationUsage( JavaType.class ) ).isNotNull();

		final FieldDetails qtyField = classDetails.findFieldByName( "quantity" );
		assertThat( qtyField.getType().determineRawClass().getClassName() ).isEqualTo( int.class.getName() );
	}

	@Test
	@ServiceRegistry
	void testSemiSimpleDynamicModel(ServiceRegistryScope registryScope) {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/dynamic/dynamic-semi-simple.xml" )
				.build();

		final SourceModelBuildingContext sourceModelBuildingContext = createBuildingContext( managedResources, registryScope.getRegistry() );

		final ClassDetailsRegistry classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();

		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( "Contact" );
		assertThat( classDetails.getClassName() ).isNull();
		assertThat( classDetails.getName() ).isEqualTo( "Contact" );

		final FieldDetails idField = classDetails.findFieldByName( "id" );
		assertThat( idField.getType().determineRawClass().getClassName() ).isEqualTo( Integer.class.getName() );

		final FieldDetails nameField = classDetails.findFieldByName( "name" );
		assertThat( nameField.getType().determineRawClass().getClassName() ).isNull();
		assertThat( nameField.getType().getName() ).isEqualTo( "Name" );
		assertThat( nameField.getDirectAnnotationUsage( Target.class ) ).isNotNull();
		assertThat( nameField.getDirectAnnotationUsage( Target.class ).value() ).isEqualTo( "Name" );

		assertThat( nameField.getType().determineRawClass().getFields() ).hasSize( 2 );

		final FieldDetails labels = classDetails.findFieldByName( "labels" );
		assertThat( labels.getType().determineRawClass().getClassName() ).isEqualTo( Set.class.getName() );
		final ElementCollection elementCollection = labels.getDirectAnnotationUsage( ElementCollection.class );
		assertThat( elementCollection.targetClass() ).isEqualTo( void.class );
		final Target targetUsage = labels.getDirectAnnotationUsage( Target.class );
		assertThat( targetUsage.value() ).isEqualTo( "string" );

		final CollectionClassification collectionClassification = labels.getDirectAnnotationUsage( CollectionClassification.class );
		assertThat( collectionClassification.value() ).isEqualTo( LimitedCollectionClassification.SET );

		assertThat( labels.getDirectAnnotationUsage( SortNatural.class ) ).isNotNull();

		final CollectionTable collectionTable = labels.getDirectAnnotationUsage( CollectionTable.class );
		assertThat( collectionTable.name() ).isEqualTo( "labels" );

		final JoinColumn[] joinColumns = collectionTable.joinColumns();
		assertThat( joinColumns ).hasSize( 1 );
		assertThat( joinColumns[0].name() ).isEqualTo( "contact_fk" );
	}

	@Test
	@ServiceRegistry
	void testIdClass(ServiceRegistryScope registryScope) {
		// todo (7.0) : how is this dynamic?

		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/dynamic/dynamic-id-class.xml" )
				.build();
		final SourceModelBuildingContext sourceModelBuildingContext = createBuildingContext( managedResources, registryScope.getRegistry() );

		final ClassDetailsRegistry classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();
		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( Employee.class.getName() );

		final IdClass idClass = classDetails.getDirectAnnotationUsage( IdClass.class );
		assertThat( idClass ).isNotNull();
		assertThat( idClass.value().getName() ).isEqualTo( EmployeePK.class.getName() );
	}

	@Test
	@ServiceRegistry
	void testOneToMany(ServiceRegistryScope registryScope) {
		// todo (7.0) : how is this dynamic?

		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/dynamic/dynamic-plurals.xml" )
				.build();
		final SourceModelBuildingContext sourceModelBuildingContext = createBuildingContext( managedResources, registryScope.getRegistry() );
		final ClassDetailsRegistry classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();

		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( Employee.class.getName() );
		assertThat( classDetails.getName() ).isEqualTo( Employee.class.getName() );

		final FieldDetails oneToMany = classDetails.findFieldByName( "addresses" );
		assertThat( oneToMany.getType().determineRawClass().getClassName() ).isEqualTo( List.class.getName() );
		final OneToMany oneToManyAnn = oneToMany.getDirectAnnotationUsage( OneToMany.class );
		assertThat( oneToManyAnn.fetch() ).isEqualTo( FetchType.EAGER );
		assertThat( oneToMany.getDirectAnnotationUsage( OnDelete.class ).action() ).isEqualTo( OnDeleteAction.CASCADE );
		final JoinColumn joinColumn = oneToMany.getAnnotationUsage( JoinColumn.class, sourceModelBuildingContext );
		assertThat( joinColumn.name() ).isEqualTo( "employee_id" );
		assertThat( joinColumn.referencedColumnName() ).isEqualTo( "emp_num" );
		assertThat( joinColumn.insertable() ).isEqualTo( Boolean.FALSE );
		assertThat( joinColumn.updatable() ).isEqualTo( Boolean.FALSE );
		final ForeignKey foreignKey = joinColumn.foreignKey();
		assertThat( foreignKey.name() ).isEqualTo( "employee_address_fk" );
		assertThat( foreignKey.value() ).isEqualTo( ConstraintMode.NO_CONSTRAINT );
		final CheckConstraint[] checkConstraints = joinColumn.check();
		assertThat( checkConstraints ).hasSize( 1 );
		assertThat( checkConstraints[0].name() ).isEqualTo( "employee_id_nn" );
		assertThat( checkConstraints[0].constraint() ).isEqualTo( "employee_id is not null" );
		assertThat( oneToMany.getDirectAnnotationUsage( Cascade.class ).value() )
				.contains( CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.LOCK );
	}
}
