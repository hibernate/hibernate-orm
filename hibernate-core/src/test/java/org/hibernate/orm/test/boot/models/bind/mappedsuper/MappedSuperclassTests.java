/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.mappedsuper;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.boot.mapping.internal.jpa.JpaStaticMetamodelInjectionSource;
import org.hibernate.annotations.TenantId;
import org.hibernate.boot.mapping.internal.model.IdentifiableAttributeDeclarationBinding;
import org.hibernate.boot.mapping.internal.model.EntityHierarchyBinding;
import org.hibernate.boot.mapping.internal.model.AttributeUsageBinding;
import org.hibernate.boot.mapping.internal.model.ManagedTypeBinding;
import org.hibernate.boot.mapping.internal.model.StandardAttributeUsageBinding;
import org.hibernate.boot.mapping.internal.view.EntityHierarchyView;
import org.hibernate.boot.mapping.internal.categorize.BasicKeyMapping;
import org.hibernate.boot.mapping.internal.categorize.EntityHierarchy;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.AppliedMappingPart;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.internal.IdentifierHandoffResolver;
import org.hibernate.metamodel.internal.MappedSuperclassHandoffResolver;
import org.hibernate.metamodel.internal.AttributeUsageHandoff;
import org.hibernate.boot.serial.internal.RuntimeMappingHandoffSnapshot;
import org.hibernate.mapping.Subclass;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.orm.test.boot.models.bind.callbacks.HierarchyRoot;
import org.hibernate.orm.test.boot.models.bind.callbacks.HierarchySuper;
import org.hibernate.mapping.DeclarationRole;
import org.hibernate.mapping.MappingRole;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Embedded;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.buildHierarchyMetadata;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class MappedSuperclassTests {
	@Test
	void testAssumptions() {
		final Set<EntityHierarchy> entityHierarchies = buildHierarchyMetadata( HierarchyRoot.class, HierarchySuper.class );
		assertThat( entityHierarchies ).hasSize( 1 );
		final EntityHierarchy entityHierarchy = entityHierarchies.iterator().next();

		assertThat( entityHierarchy.getIdMapping() ).isNotNull();
		final BasicKeyMapping idMapping = (BasicKeyMapping) entityHierarchy.getIdMapping();
		assertThat( idMapping.getAttribute().getMember().getDirectAnnotationUsage( Id.class ) ).isNotNull();
		assertThat( idMapping.getAttribute().getMember().getDirectAnnotationUsage( EmbeddedId.class ) ).isNull();

		assertThat( entityHierarchy.getVersionAttribute() ).isNotNull();
		assertThat( entityHierarchy.getVersionAttribute().getMember().getDirectAnnotationUsage( Version.class ) ).isNotNull();

		assertThat( entityHierarchy.getTenantIdAttribute() ).isNotNull();
		assertThat( entityHierarchy.getTenantIdAttribute().getMember().getDirectAnnotationUsage( TenantId.class ) ).isNotNull();

		assertThat( entityHierarchy.getCacheRegion() ).isNotNull();
		assertThat( entityHierarchy.getCacheRegion().getAccessType() ).isEqualTo( AccessType.READ_ONLY );

		assertThat( entityHierarchy.getInheritanceType() ).isNotNull();
		assertThat( entityHierarchy.getInheritanceType() ).isEqualTo( InheritanceType.JOINED );
	}

	@Test
	@ServiceRegistry
	void testBindings(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();
					final MappedSuperclass superBinding = metadataCollector.getMappedSuperclass( HierarchySuper.class );
					final PersistentClass rootBinding = metadataCollector.getEntityBinding( HierarchyRoot.class.getName() );
					final EntityHierarchy categorizedHierarchy = context.getCategorizedDomainModel()
							.getEntityHierarchies()
							.iterator()
							.next();
					final EntityHierarchyView hierarchyView = context.getBindingState()
							.getBootBindingModel()
							.getEntityHierarchyView( categorizedHierarchy.getRoot() );

					assertThat( rootBinding.getMappedClass() ).isEqualTo( HierarchyRoot.class );
					assertThat( rootBinding.getSuperMappedSuperclass() ).isSameAs( superBinding );
					assertThat( rootBinding.getSuperType() ).isSameAs( superBinding );
					assertThat( superBinding.getSubTypes() ).containsExactly( rootBinding );
					assertThat( rootBinding.getImplicitTable() ).isNotNull();
					assertThat( rootBinding.getTable() ).isNotNull();
					assertThat( rootBinding.getRootTable() ).isSameAs( rootBinding.getTable() );
					assertThat( rootBinding.getIdentityTable() ).isSameAs( rootBinding.getTable() );
					assertThat( rootBinding.getIdentityTable().getPrimaryKey() ).isNotNull();
					assertThat( rootBinding.getIdentityTable().getPrimaryKey().getColumns() ).hasSize( 1 );
					assertThat( rootBinding.getIdentifier() ).isNotNull();
					assertThat( rootBinding.getIdentifierProperty() ).isNotNull();
					assertThat( rootBinding.getIdentifierProperty().getColumns() ).hasSize( 1 );
					assertThat( ( (AppliedMappingPart) rootBinding.getIdentifier() ).getMappingRole() )
							.isEqualTo( MappingRole.entity( HierarchyRoot.class.getName() )
									.append( MappingRole.PartKind.IDENTIFIER ) );
					assertThat( rootBinding.getVersion().getMappingRole() )
							.isEqualTo( MappingRole.entity( HierarchyRoot.class.getName() )
									.append( MappingRole.PartKind.VERSION ) );

					assertThat( hierarchyView ).isNotNull();
					assertThat( hierarchyView.root().classDetails().toJavaClass() ).isEqualTo( HierarchyRoot.class );
					assertThat( hierarchyView.absoluteRoot().classDetails().toJavaClass() ).isEqualTo( HierarchySuper.class );
					assertThat( hierarchyView.inheritanceType() ).isEqualTo( InheritanceType.JOINED );
					assertThat( hierarchyView.entityTypes() )
							.extracting( (type) -> (Object) type.classDetails().toJavaClass() )
							.containsExactly( HierarchyRoot.class );
					assertThat( hierarchyView.mappedSuperclassTypes() )
							.extracting( (type) -> (Object) type.classDetails().toJavaClass() )
							.containsExactly( HierarchySuper.class );
					assertThat( hierarchyView.types() )
							.extracting( EntityHierarchyBinding.Type::relation )
							.containsExactly( EntityHierarchyBinding.Relation.SUPER, EntityHierarchyBinding.Relation.ROOT );

					final JpaStaticMetamodelInjectionSource metamodelInjectionSource =
							JpaStaticMetamodelInjectionSource.from( context.getBindingState().getBootBindingModel() );
					assertThat( metamodelInjectionSource.managedTypes() )
							.extracting( JpaStaticMetamodelInjectionSource.ManagedTypeReference::javaType )
							.containsExactly( HierarchySuper.class, HierarchyRoot.class );

					final JpaStaticMetamodelInjectionSource.ManagedTypeReference superTypeReference =
							metamodelInjectionSource.managedTypes().get( 0 );
					assertThat( superTypeReference.classDetails().toJavaClass() )
							.isEqualTo( HierarchySuper.class );
					assertThat( superTypeReference.kind() )
							.isEqualTo( ManagedTypeBinding.Kind.MAPPED_SUPERCLASS );
					assertThat( superTypeReference.fieldNames() )
							.contains( "id", "name" );
					assertThat( superTypeReference.fields() )
							.extracting( JpaStaticMetamodelInjectionSource.FieldReference::role )
							.contains(
									JpaStaticMetamodelInjectionSource.FieldRole.IDENTIFIER_ATTRIBUTE,
									JpaStaticMetamodelInjectionSource.FieldRole.DECLARED_ATTRIBUTE
							);
					assertThat( superTypeReference.fields() )
							.filteredOn( (field) -> field.fieldName().equals( "id" ) )
							.first()
							.isInstanceOf( JpaStaticMetamodelInjectionSource.IdentifierFieldReference.class );
					assertThat( superTypeReference.fields() )
							.filteredOn( (field) -> field.fieldName().equals( "name" ) )
							.first()
							.isInstanceOf( JpaStaticMetamodelInjectionSource.DeclaredAttributeFieldReference.class );
					assertThat( metamodelInjectionSource.managedTypes().get( 1 ).kind() )
							.isEqualTo( ManagedTypeBinding.Kind.ENTITY );
				},
				scope.getRegistry(),
				HierarchyRoot.class,
				HierarchySuper.class
		);
	}

	@Test
	@ServiceRegistry
	void appliesMappedSuperclassPropertiesToNearestEntityConsumer(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();
					final MappedSuperclass contributionBinding = metadataCollector.getMappedSuperclass( RootContribution.class );
					final PersistentClass rootBinding = metadataCollector.getEntityBinding( RootConsumer.class.getName() );
					final PersistentClass leafBinding = metadataCollector.getEntityBinding( LeafConsumer.class.getName() );

					assertThat( rootBinding.getSuperType() ).isSameAs( contributionBinding );
					assertThat( contributionBinding.getSubTypes() ).containsExactly( rootBinding );
					assertThat( leafBinding.getSuperclass() ).isSameAs( rootBinding );
					assertThat( rootBinding.getDirectSubclasses() ).containsExactly( (Subclass) leafBinding );
					assertThat( leafBinding.getSuperType() ).isSameAs( rootBinding );
					assertThat( rootBinding.getSubTypes() ).containsExactly( leafBinding );
					final Property rootCode = localProperty( rootBinding, "rootCode" );
					assertThat( rootBinding.getDeclaredProperties() ).doesNotContain( rootCode );
					assertThat( rootBinding.getMappedSuperclassProperties() ).containsExactly( rootCode );
					final var bootBindingModel = context.getBindingState().getBootBindingModel();
					final var appliedMapping =
							bootBindingModel.getAppliedAttributeMapping( rootCode.getMappingRole() );
					assertThat( appliedMapping ).isNotNull();
					assertThat( bootBindingModel.mappedSuperclassContributions() )
							.singleElement()
							.satisfies( (contribution) -> {
								assertThat( contribution.declaration().getClassDetails().toJavaClass() )
										.isEqualTo( RootContribution.class );
								assertThat( contribution.consumer().getClassDetails().toJavaClass() )
										.isEqualTo( RootConsumer.class );
								assertThat( contribution.appliedAttributeNames() ).containsExactly( "rootCode" );
								assertThat( contribution.appliedAttributeUsages() ).containsExactly( appliedMapping.usage() );
								assertThat( appliedMapping.declaration().declarationContainer().classDetails().toJavaClass() )
										.isEqualTo( RootContribution.class );
								assertThat( ( (ManagedTypeBinding) appliedMapping.usage().usageContainer() )
										.classDetails()
										.toJavaClass() )
										.isEqualTo( RootConsumer.class );
							} );
					final var resolver = new MappedSuperclassHandoffResolver(
							RuntimeMappingHandoffSnapshot.from( bootBindingModel, context.getMetadata() )
					);
					assertHandoffMatches(
							resolver.findAttributeUsage( rootBinding, rootCode ),
							appliedMapping.usage()
					);
					assertHandoffMatches(
							resolver.findAttributeUsage( leafBinding, rootCode ),
							appliedMapping.usage()
					);
					final Property declarationCopy =
							rootCode.copyForDeclaration( rootCode.getValue().copy() );
					assertThat( declarationCopy.getDeclarationRole() ).isEqualTo( rootCode.getDeclarationRole() );
					assertThat( declarationCopy.getMappingRole() ).isNull();
					assertThat( declarationCopy.getValue() ).isNotSameAs( rootCode.getValue() );
					assertThat( ( (AppliedMappingPart) declarationCopy.getValue() ).getMappingRole() ).isNull();
					assertThatThrownBy( () -> rootCode.copyForDeclaration( rootCode.getValue() ) )
							.isInstanceOf( IllegalArgumentException.class )
							.hasMessageContaining( rootCode.getMappingRole().getFullPath() );
					final Property declarationView = rootCode.copyForDeclarationView();
					assertThat( declarationView.getDeclarationRole() ).isEqualTo( rootCode.getDeclarationRole() );
					assertThat( declarationView.getMappingRole() ).isNull();
					assertThat( declarationView.getValue() ).isSameAs( rootCode.getValue() );
					final Property sameApplicationCopy = rootCode.copyForSameApplication();
					assertThat( sameApplicationCopy.getMappingRole() ).isEqualTo( rootCode.getMappingRole() );
					assertThat( sameApplicationCopy.getValue() ).isSameAs( rootCode.getValue() );
					final var copiedValue = rootCode.getValue().copy();
					final MappingRole otherApplicationRole =
							MappingRole.entity( "OtherRoot" ).appendAttribute( rootCode.getName() );
					final Property otherApplication =
							rootCode.copyForApplication( otherApplicationRole, copiedValue );
					assertThat( otherApplication.getDeclarationRole() ).isEqualTo( rootCode.getDeclarationRole() );
					assertThat( otherApplication.getMappingRole() ).isEqualTo( otherApplicationRole );
					assertThat( otherApplication.getValue() ).isSameAs( copiedValue );
					assertThat( ( (AppliedMappingPart) copiedValue ).getMappingRole() )
							.isEqualTo( otherApplicationRole );
					assertThat( ( (AppliedMappingPart) rootCode.getValue() ).getMappingRole() )
							.isEqualTo( rootCode.getMappingRole() );
					assertThat( countLocalProperties( rootBinding, "rootCode" ) ).isEqualTo( 1 );
					assertThat( countLocalProperties( leafBinding, "rootCode" ) ).isEqualTo( 0 );
					assertThat( countClosureProperties( leafBinding, "rootCode" ) ).isEqualTo( 1 );
				},
				scope.getRegistry(),
				RootContribution.class,
				RootConsumer.class,
				LeafConsumer.class
		);
	}

	@Test
	@ServiceRegistry
	void appliesMappedSuperclassBetweenEntitiesToNearestSubclassEntity(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();
					final PersistentClass rootBinding = metadataCollector.getEntityBinding( EntityBeforeContribution.class.getName() );
					final MappedSuperclass contributionBinding = metadataCollector.getMappedSuperclass( ContributionBetweenEntities.class );
					final PersistentClass appliedBinding = metadataCollector.getEntityBinding( EntityAfterContribution.class.getName() );
					final PersistentClass leafBinding = metadataCollector.getEntityBinding( EntityAfterContributionLeaf.class.getName() );

					assertThat( contributionBinding.getSuperType() ).isSameAs( rootBinding );
					assertThat( rootBinding.getSubTypes() ).containsExactly( contributionBinding );
					assertThat( appliedBinding.getSuperType() ).isSameAs( contributionBinding );
					assertThat( contributionBinding.getSubTypes() ).containsExactly( appliedBinding );
					assertThat( leafBinding.getSuperType() ).isSameAs( appliedBinding );
					assertThat( appliedBinding.getSubTypes() ).containsExactly( leafBinding );

					final Property nestedCode = localProperty( appliedBinding, "nestedCode" );
					assertThat( appliedBinding.getDeclaredProperties() ).doesNotContain( nestedCode );
					assertThat( appliedBinding.getMappedSuperclassProperties() ).containsExactly( nestedCode );
					final var bootBindingModel = context.getBindingState().getBootBindingModel();
					final var appliedMapping =
							bootBindingModel.getAppliedAttributeMapping( nestedCode.getMappingRole() );
					assertThat( appliedMapping ).isNotNull();
					assertThat( bootBindingModel.mappedSuperclassContributions() )
							.singleElement()
							.satisfies( (contribution) -> {
								assertThat( contribution.declaration().getClassDetails().toJavaClass() )
										.isEqualTo( ContributionBetweenEntities.class );
								assertThat( contribution.consumer().getClassDetails().toJavaClass() )
										.isEqualTo( EntityAfterContribution.class );
								assertThat( contribution.appliedAttributeNames() ).containsExactly( "nestedCode" );
								assertThat( contribution.appliedAttributeUsages() )
										.containsExactly( appliedMapping.usage() );
							} );
					assertHandoffMatches(
							new MappedSuperclassHandoffResolver(
									RuntimeMappingHandoffSnapshot.from( bootBindingModel, context.getMetadata() )
							).findAttributeUsage( appliedBinding, nestedCode ),
							appliedMapping.usage()
					);
					assertThat( countLocalProperties( rootBinding, "nestedCode" ) ).isEqualTo( 0 );
					assertThat( countLocalProperties( appliedBinding, "nestedCode" ) ).isEqualTo( 1 );
					assertThat( countLocalProperties( leafBinding, "nestedCode" ) ).isEqualTo( 0 );
					assertThat( countClosureProperties( leafBinding, "nestedCode" ) ).isEqualTo( 1 );
				},
				scope.getRegistry(),
				EntityBeforeContribution.class,
				ContributionBetweenEntities.class,
				EntityAfterContribution.class,
				EntityAfterContributionLeaf.class
		);
	}

	@Test
	@ServiceRegistry
	void sharedMappedSuperclassUsesSeparateAppliedPropertyCopies(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();
					final PersistentClass firstRootBinding = metadataCollector.getEntityBinding( FirstSharedRoot.class.getName() );
					final PersistentClass secondRootBinding = metadataCollector.getEntityBinding( SecondSharedRoot.class.getName() );
					final MappedSuperclass firstContribution = (MappedSuperclass) firstRootBinding.getSuperType();
					final MappedSuperclass secondContribution = (MappedSuperclass) secondRootBinding.getSuperType();
					final Property firstAppliedProperty = localProperty( firstRootBinding, "sharedCode" );
					final Property secondAppliedProperty = localProperty( secondRootBinding, "sharedCode" );

					assertThat( firstContribution.getMappedClass() ).isEqualTo( SharedContribution.class );
					assertThat( secondContribution.getMappedClass() ).isEqualTo( SharedContribution.class );
					assertThat( firstContribution ).isNotSameAs( secondContribution );
					assertThat( firstContribution.getSubTypes() ).containsExactly( firstRootBinding );
					assertThat( secondContribution.getSubTypes() ).containsExactly( secondRootBinding );
					assertThat( firstAppliedProperty ).isNotNull();
					assertThat( secondAppliedProperty ).isNotNull();
					assertThat( firstAppliedProperty ).isNotSameAs( secondAppliedProperty );
					assertThat( firstAppliedProperty.getDeclarationRole() )
							.isEqualTo( new DeclarationRole( SharedContribution.class.getName(), "sharedCode" ) );
					assertThat( secondAppliedProperty.getDeclarationRole() )
							.isEqualTo( firstAppliedProperty.getDeclarationRole() );
					assertThat( firstAppliedProperty.getMappingRole() )
							.isEqualTo( MappingRole.entity( FirstSharedRoot.class.getName() )
									.appendAttribute( "sharedCode" ) );
					assertThat( secondAppliedProperty.getMappingRole() )
							.isEqualTo( MappingRole.entity( SecondSharedRoot.class.getName() )
									.appendAttribute( "sharedCode" ) );
					final var bootBindingModel = context.getBindingState().getBootBindingModel();
					final var firstAppliedMapping =
							bootBindingModel.getAppliedAttributeMapping( firstAppliedProperty.getMappingRole() );
					final var secondAppliedMapping =
							bootBindingModel.getAppliedAttributeMapping( secondAppliedProperty.getMappingRole() );
					assertThat( firstAppliedMapping ).isNotNull();
					assertThat( secondAppliedMapping ).isNotNull();
					assertThat( firstAppliedMapping ).isNotSameAs( secondAppliedMapping );
					assertThat( firstAppliedMapping.usage() ).isNotSameAs( secondAppliedMapping.usage() );
					assertThat( firstAppliedMapping.declaration() ).isSameAs( secondAppliedMapping.declaration() );
					assertThat( firstAppliedMapping.declarationRole() )
							.isEqualTo( new DeclarationRole( SharedContribution.class.getName(), "sharedCode" ) );
					assertThat( firstRootBinding.getMappedSuperclassProperties() ).containsExactly( firstAppliedProperty );
					assertThat( secondRootBinding.getMappedSuperclassProperties() ).containsExactly( secondAppliedProperty );
					final var resolver = new MappedSuperclassHandoffResolver(
							RuntimeMappingHandoffSnapshot.from( bootBindingModel, context.getMetadata() )
					);
					assertHandoffMatches(
							resolver.findAttributeUsage( firstRootBinding, firstAppliedProperty ),
							firstAppliedMapping.usage()
					);
					assertHandoffMatches(
							resolver.findAttributeUsage( secondRootBinding, secondAppliedProperty ),
							secondAppliedMapping.usage()
					);
					assertThat( bootBindingModel.mappedSuperclassContributions() )
							.extracting( "appliedAttributeUsages" )
							.containsExactly(
									java.util.List.of( firstAppliedMapping.usage() ),
									java.util.List.of( secondAppliedMapping.usage() )
							);
				},
				scope.getRegistry(),
				SharedContribution.class,
				FirstSharedRoot.class,
				SecondSharedRoot.class
		);
	}

	@Test
	@ServiceRegistry
	void mappedSuperclassComponentSeparatesDeclarationAndAppliedRoles(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();
					final MappedSuperclass declaration =
							metadataCollector.getMappedSuperclass( SharedEmbeddedContribution.class );
					final PersistentClass first =
							metadataCollector.getEntityBinding( FirstEmbeddedRoot.class.getName() );
					final PersistentClass second =
							metadataCollector.getEntityBinding( SecondEmbeddedRoot.class.getName() );

					final Property declarationProperty = declaration.getDeclaredProperties()
							.stream()
							.filter( (property) -> property.getName().equals( "details" ) )
							.findFirst()
							.orElseThrow();
					final Component declarationComponent = (Component) declarationProperty.getValue();
					final Component firstComponent = (Component) first.getProperty( "details" ).getValue();
					final Component secondComponent = (Component) second.getProperty( "details" ).getValue();

					assertThat( declarationProperty.getDeclarationRole() )
							.isEqualTo( new DeclarationRole( SharedEmbeddedContribution.class.getName(), "details" ) );
					assertThat( declarationProperty.getMappingRole() ).isNull();
					assertThat( declarationComponent.getMappingRole() ).isNull();
					assertThat( firstComponent.getMappingRole() )
							.isEqualTo( MappingRole.entity( FirstEmbeddedRoot.class.getName() )
									.appendAttribute( "details" ) );
					assertThat( secondComponent.getMappingRole() )
							.isEqualTo( MappingRole.entity( SecondEmbeddedRoot.class.getName() )
									.appendAttribute( "details" ) );
					assertThat( firstComponent.getProperty( "value" ).getMappingRole() )
							.isEqualTo( firstComponent.getMappingRole().appendAttribute( "value" ) );
					assertThat( secondComponent.getProperty( "value" ).getMappingRole() )
							.isEqualTo( secondComponent.getMappingRole().appendAttribute( "value" ) );
					final Component declarationCopy = firstComponent.copyForDeclaration(
							firstComponent.getProperties()
									.stream()
									.map( (property) -> property.copyForDeclaration( property.getValue().copy() ) )
									.toList()
					);
					assertThat( declarationCopy.getMappingRole() ).isNull();
					assertThat( declarationCopy.getProperties() )
							.noneMatch( (property) -> property.getMappingRole() != null );
					final Component sameApplicationCopy = firstComponent.copyForSameApplication();
					assertThat( sameApplicationCopy.getMappingRole() ).isEqualTo( firstComponent.getMappingRole() );
					assertThat( sameApplicationCopy.getProperties() )
							.containsExactlyElementsOf( firstComponent.getProperties() );
					final MappingRole otherApplicationRole =
							MappingRole.entity( FirstEmbeddedRoot.class.getName() )
									.appendAttribute( "otherDetails" );
					final java.util.List<Property> otherApplicationProperties =
							firstComponent.getProperties()
									.stream()
									.map( (property) -> property.copyForApplication(
											otherApplicationRole.appendAttribute( property.getName() ),
											property.getValue().copy()
									) )
									.toList();
					final Component otherApplication = firstComponent.copyForApplication(
							context.getBindingState().getMetadataBuildingContext(),
							otherApplicationRole,
							otherApplicationProperties
					);
					assertThat( otherApplication.getMappingRole() ).isEqualTo( otherApplicationRole );
					assertThat( otherApplication.getTable() ).isSameAs( firstComponent.getTable() );
					assertThat( otherApplication.getOwner() ).isSameAs( firstComponent.getOwner() );
					assertThat( otherApplication.getProperties() ).containsExactlyElementsOf( otherApplicationProperties );
					assertThat( otherApplication.getProperties().get( 0 ) )
							.isNotSameAs( firstComponent.getProperties().get( 0 ) );
					assertThat( otherApplication.getProperties().get( 0 ).getValue() )
							.isNotSameAs( firstComponent.getProperties().get( 0 ).getValue() );
					assertThat( otherApplication.getProperties().get( 0 ).getMappingRole() )
							.isEqualTo( otherApplicationRole.appendAttribute(
									otherApplication.getProperties().get( 0 ).getName()
							) );
					assertThatThrownBy( () -> firstComponent.copyForApplication(
							context.getBindingState().getMetadataBuildingContext(),
							otherApplicationRole,
							firstComponent.getProperties()
					) )
							.isInstanceOf( IllegalArgumentException.class )
							.hasMessageContaining( "different mapping role" );
					final var bootBindingModel = context.getBindingState().getBootBindingModel();
					assertThat( bootBindingModel.getAppliedAttributeMapping(
							MappingRole.mappedSuperclass( SharedEmbeddedContribution.class.getName() )
									.appendAttribute( "details" )
					) ).isNull();
					assertThat( bootBindingModel.getAppliedEmbeddableMapping(
							MappingRole.mappedSuperclass( SharedEmbeddedContribution.class.getName() )
									.appendAttribute( "details" )
					) ).isNull();
					final var firstAppliedDetails =
							bootBindingModel.getAppliedAttributeMapping( firstComponent.getMappingRole() );
					final var secondAppliedDetails =
							bootBindingModel.getAppliedAttributeMapping( secondComponent.getMappingRole() );
					assertThat( firstAppliedDetails ).isNotNull();
					assertThat( secondAppliedDetails ).isNotNull();
					assertThat( firstAppliedDetails.declaration() ).isSameAs( secondAppliedDetails.declaration() );
					final var firstAppliedComponent =
							bootBindingModel.getAppliedEmbeddableMapping( firstComponent.getMappingRole() );
					final var secondAppliedComponent =
							bootBindingModel.getAppliedEmbeddableMapping( secondComponent.getMappingRole() );
					assertThat( firstAppliedComponent ).isNotNull();
					assertThat( secondAppliedComponent ).isNotNull();
					assertThat( firstAppliedComponent ).isNotSameAs( secondAppliedComponent );
					assertThat( firstAppliedComponent.componentType() )
							.isSameAs( secondAppliedComponent.componentType() );
					assertThat( firstAppliedComponent.findAttribute( "value" ) )
							.isSameAs( bootBindingModel.getAppliedAttributeMapping(
									firstComponent.getMappingRole().appendAttribute( "value" )
							) );
					assertThat( secondAppliedComponent.findAttribute( "value" ) )
							.isSameAs( bootBindingModel.getAppliedAttributeMapping(
									secondComponent.getMappingRole().appendAttribute( "value" )
							) );
					assertThat( bootBindingModel.getAppliedAttributeMapping(
							firstComponent.getMappingRole().appendAttribute( "value" )
					).containerRole() ).isEqualTo( firstComponent.getMappingRole() );
					assertThat( bootBindingModel.getAppliedAttributeMapping(
							secondComponent.getMappingRole().appendAttribute( "value" )
					).containerRole() ).isEqualTo( secondComponent.getMappingRole() );
				},
				scope.getRegistry(),
				SharedEmbeddedContribution.class,
				SharedDetails.class,
				FirstEmbeddedRoot.class,
				SecondEmbeddedRoot.class
		);
	}

	@Test
	@ServiceRegistry
	void genericMappedSuperclassAttributeIsStoredAsSpecializedEntityUsage(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final ManagedTypeBinding entityBinding = managedTypeBinding( context, GenericStringEntity.class );
					final ManagedTypeBinding superBinding = managedTypeBinding( context, GenericMappedSuper.class );
					final AttributeUsageBinding usage = attributeUsage( entityBinding, "genericValue" );
					final var metadataCollector = context.getMetadataCollector();
					final PersistentClass persistentClass =
							metadataCollector.getEntityBinding( GenericStringEntity.class.getName() );
					final Property genericValueProperty = persistentClass.getProperty( "genericValue" );
					final var bootBindingModel = context.getBindingState().getBootBindingModel();
					final var appliedMapping =
							bootBindingModel.getAppliedAttributeMapping( genericValueProperty.getMappingRole() );

					assertThat( usage ).isInstanceOf( StandardAttributeUsageBinding.class );
					assertThat( usage.declaration() ).isInstanceOf( IdentifiableAttributeDeclarationBinding.class );
					assertThat( usage.declaration() ).isSameAs( superBinding.declaredAttributes().get( 0 ) );
					assertThat( usage.resolvedType().determineRawClass().toJavaClass() ).isEqualTo( String.class );
					assertThat( appliedMapping ).isNotNull();
					assertThat( appliedMapping.usage() ).isSameAs( usage );
					assertThat( appliedMapping.declaration() ).isSameAs( usage.declaration() );
					assertThat( bootBindingModel.mappedSuperclassContributions() )
							.singleElement()
							.satisfies( (contribution) -> {
								assertThat( contribution.declaration().getClassDetails().toJavaClass() )
										.isEqualTo( GenericMappedSuper.class );
								assertThat( contribution.consumer().getClassDetails().toJavaClass() )
										.isEqualTo( GenericStringEntity.class );
								assertThat( contribution.appliedAttributeUsages() ).containsExactly( usage );
							} );
					final var resolver = new MappedSuperclassHandoffResolver(
							RuntimeMappingHandoffSnapshot.from( bootBindingModel, context.getMetadata() )
					);
					assertHandoffMatches(
							resolver.findAttributeUsage( persistentClass, genericValueProperty ),
							usage
					);
					final Property declarationProperty = metadataCollector
							.getMappedSuperclass( GenericMappedSuper.class )
							.getDeclaredProperties()
							.get( 0 );
					assertThat( declarationProperty.getMappingRole() ).isNull();
					assertHandoffMatches(
							resolver.findAttributeUsage( persistentClass, declarationProperty ),
							usage
					);
					assertThat( entityBinding.declaredAttributes() )
							.extracting( "attributeName" )
							.doesNotContain( "genericValue" );
					assertThat( superBinding.declaredAttributes() )
							.extracting( "attributeName" )
							.contains( "genericValue" );
				},
				scope.getRegistry(),
				GenericMappedSuper.class,
				GenericStringEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void genericMappedSuperclassToOneAttributesAreIndexedByMappingRole(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var bootBindingModel = context.getBindingState().getBootBindingModel();
					final PersistentClass parent =
							context.getMetadataCollector().getEntityBinding( ToOneParent.class.getName() );
					final PersistentClass child =
							context.getMetadataCollector().getEntityBinding( ToOneChild.class.getName() );
					final Property childProperty = parent.getProperty( "child" );
					final Property parentProperty = child.getProperty( "parent" );
					final AttributeUsageBinding childUsage = bootBindingModel
							.getAppliedAttributeMapping( childProperty.getMappingRole() )
							.usage();
					final AttributeUsageBinding parentUsage = bootBindingModel
							.getAppliedAttributeMapping( parentProperty.getMappingRole() )
							.usage();

					assertThat( childUsage ).isNotNull();
					assertThat( childUsage.resolvedType().determineRawClass().toJavaClass() )
							.isEqualTo( ToOneChild.class );
					assertThat( parentUsage ).isNotNull();
					assertThat( parentUsage.resolvedType().determineRawClass().toJavaClass() )
							.isEqualTo( ToOneParent.class );
					final var resolver = new MappedSuperclassHandoffResolver(
							RuntimeMappingHandoffSnapshot.from( bootBindingModel, context.getMetadata() )
					);
					assertHandoffMatches( resolver.findAttributeUsage( parent, childProperty ), childUsage );
					assertHandoffMatches( resolver.findAttributeUsage( child, parentProperty ), parentUsage );
				},
				scope.getRegistry(),
				GenericToOneParent.class,
				ToOneParent.class,
				GenericToOneChild.class,
				ToOneChild.class
		);
	}

	@Test
	@ServiceRegistry
	void genericMappedSuperclassPluralAttributeIsStoredAsGenericDeclaration(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();
					final MappedSuperclass superBinding =
							metadataCollector.getMappedSuperclass( GenericCollectionMappedSuper.class );
					final PersistentClass entityBinding =
							metadataCollector.getEntityBinding( GenericCollectionEntity.class.getName() );

					assertThat( superBinding.getDeclaredProperties() )
							.filteredOn( (property) -> property.getName().equals( "genericChildren" ) )
							.singleElement()
							.satisfies( (property) -> {
								assertThat( property.isGeneric() ).isTrue();
								assertThat( property.getMappingRole() ).isNull();
								assertThat( ( (org.hibernate.mapping.Collection) property.getValue() ).getMappingRole() )
										.isNull();
							} );
					final Property appliedProperty = entityBinding.getProperty( "genericChildren" );
					final org.hibernate.mapping.Collection appliedCollection =
							(org.hibernate.mapping.Collection) appliedProperty.getValue();
					assertThat( appliedProperty.isGeneric() ).isFalse();
					assertThat( appliedProperty.getMappingRole() )
							.isEqualTo( MappingRole.entity( GenericCollectionEntity.class.getName() )
									.appendAttribute( "genericChildren" ) );
					assertThat( appliedCollection.getMappingRole() )
							.isEqualTo( MappingRole.collection( appliedCollection.getRole() ) );
				},
				scope.getRegistry(),
				GenericCollectionMappedSuper.class,
				GenericCollectionEntity.class,
				GenericCollectionChild.class
		);
	}

	@Test
	@ServiceRegistry
	void genericMappedSuperclassIdentifierIsResolvedFromIdentifierBinding(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var bootBindingModel = context.getBindingState().getBootBindingModel();
					final var identifierBinding = bootBindingModel.findEntityIdentifierBinding(
							GenericIntegerIdEntity.class.getName()
					);
					final PersistentClass persistentClass = context.getMetadataCollector()
							.getEntityBinding( GenericIntegerIdEntity.class.getName() );
					final Property identifierProperty = persistentClass.getIdentifierProperty();

					assertThat( identifierBinding ).isNotNull();
					assertThat( identifierBinding.identifierMember() ).isNotNull();
					assertThat( identifierBinding.identifierMember().getType().getTypeKind() )
							.isEqualTo( TypeDetails.Kind.TYPE_VARIABLE );
					assertThat( new IdentifierHandoffResolver(
							RuntimeMappingHandoffSnapshot.from( bootBindingModel, context.getMetadata() )
					)
							.isConcreteGenericIdentifier( persistentClass, identifierProperty ) )
							.isTrue();
				},
				scope.getRegistry(),
				GenericIdentifierMappedSuper.class,
				GenericIntegerIdEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void genericMappedSuperclassIdentifierWithExplicitEntityNameIsResolvedFromIdentifierBinding(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var bootBindingModel = context.getBindingState().getBootBindingModel();
					final var identifierBinding = bootBindingModel.findEntityIdentifierBinding(
							"ExplicitGenericIdEntity"
					);
					final PersistentClass persistentClass = context.getMetadataCollector()
							.getEntityBinding( ExplicitGenericIdEntity.class.getName() );
					final Property identifierProperty = persistentClass.getIdentifierProperty();

					assertThat( identifierBinding ).isNotNull();
					assertThat( identifierBinding.identifierMember() ).isNotNull();
					assertThat( identifierBinding.identifierMember().getType().getTypeKind() )
							.isEqualTo( TypeDetails.Kind.TYPE_VARIABLE );
					assertThat( new IdentifierHandoffResolver(
							RuntimeMappingHandoffSnapshot.from( bootBindingModel, context.getMetadata() )
					)
							.isConcreteGenericIdentifier( persistentClass, identifierProperty ) )
							.isTrue();
				},
				scope.getRegistry(),
				GenericIdentifierAndDataMappedSuper.class,
				ExplicitGenericIdEntity.class
		);
	}

	private static long countLocalProperties(PersistentClass binding, String propertyName) {
		return binding.getProperties().stream()
				.filter( (property) -> propertyName.equals( property.getName() ) )
				.count();
	}

	private static long countClosureProperties(PersistentClass binding, String propertyName) {
		return binding.getPropertyClosure().stream()
				.filter( (property) -> propertyName.equals( property.getName() ) )
				.count();
	}

	private static Property localProperty(PersistentClass binding, String propertyName) {
		return binding.getProperties().stream()
				.filter( (property) -> propertyName.equals( property.getName() ) )
				.findFirst()
				.orElse( null );
	}

	private static ManagedTypeBinding managedTypeBinding(
			org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.DomainModelCheckContext context,
			Class<?> javaType) {
		return context.getBindingState().getBootBindingModel()
				.managedTypeBindings()
				.stream()
				.filter( (binding) -> binding.classDetails().toJavaClass().equals( javaType ) )
				.findFirst()
				.orElseThrow();
	}

	private static AttributeUsageBinding attributeUsage(ManagedTypeBinding binding, String attributeName) {
		return binding.attributeUsages()
				.stream()
				.filter( (usage) -> usage.attributeName().equals( attributeName ) )
				.findFirst()
				.orElseThrow();
	}

	private static void assertHandoffMatches(
			AttributeUsageHandoff handoff,
			AttributeUsageBinding usage) {
		assertThat( handoff ).isNotNull();
		assertThat( handoff.member() ).isSameAs( usage.member() );
		assertThat( handoff.declaredType() ).isSameAs( usage.declaration().member().getType() );
		assertThat( handoff.usageType() ).isSameAs( usage.resolvedType() );
	}

	@jakarta.persistence.MappedSuperclass
	public static class RootContribution {
		@Id
		private Integer id;
		private String rootCode;
	}

	@Entity
	public static class RootConsumer extends RootContribution {
	}

	@Entity
	public static class LeafConsumer extends RootConsumer {
	}

	@Entity
	public static class EntityBeforeContribution {
		@Id
		private Integer id;
	}

	@jakarta.persistence.MappedSuperclass
	public static class ContributionBetweenEntities extends EntityBeforeContribution {
		private String nestedCode;
	}

	@Entity
	public static class EntityAfterContribution extends ContributionBetweenEntities {
	}

	@Entity
	public static class EntityAfterContributionLeaf extends EntityAfterContribution {
	}

	@jakarta.persistence.MappedSuperclass
	public static class SharedContribution {
		@Id
		private Integer id;
		private String sharedCode;
	}

	@Entity
	public static class FirstSharedRoot extends SharedContribution {
	}

	@Entity
	public static class SecondSharedRoot extends SharedContribution {
	}

	@Embeddable
	public static class SharedDetails {
		private String value;
	}

	@jakarta.persistence.MappedSuperclass
	public static class SharedEmbeddedContribution {
		@Id
		private Integer id;

		@Embedded
		private SharedDetails details;
	}

	@Entity
	public static class FirstEmbeddedRoot extends SharedEmbeddedContribution {
	}

	@Entity
	public static class SecondEmbeddedRoot extends SharedEmbeddedContribution {
	}

	@jakarta.persistence.MappedSuperclass
	public static class GenericMappedSuper<T> {
		@Id
		private Integer id;
		private T genericValue;
	}

	@Entity
	public static class GenericStringEntity extends GenericMappedSuper<String> {
	}

	@jakarta.persistence.MappedSuperclass
	public abstract static class GenericToOneParent<T> {
		@OneToOne
		private T child;
	}

	@Entity( name = "ToOneParent" )
	public static class ToOneParent extends GenericToOneParent<ToOneChild> {
		@Id
		private Long id;
	}

	@jakarta.persistence.MappedSuperclass
	public abstract static class GenericToOneChild<T> {
		@OneToOne
		private T parent;
	}

	@Entity( name = "ToOneChild" )
	public static class ToOneChild extends GenericToOneChild<ToOneParent> {
		@Id
		private Long id;
	}

	@jakarta.persistence.MappedSuperclass
	public static class GenericIdentifierMappedSuper<T> {
		@Id
		private T id;
	}

	@Entity
	public static class GenericIntegerIdEntity extends GenericIdentifierMappedSuper<Integer> {
	}

	@jakarta.persistence.MappedSuperclass
	public static class GenericIdentifierAndDataMappedSuper<T, S> {
		@Id
		private T id;
		private S data;
	}

	@Entity(name = "ExplicitGenericIdEntity")
	public static class ExplicitGenericIdEntity extends GenericIdentifierAndDataMappedSuper<Integer, String> {
	}

	@jakarta.persistence.MappedSuperclass
	public static class GenericCollectionMappedSuper<C extends GenericCollectionChild> {
		@Id
		private Integer id;

		@OneToMany(mappedBy = "parent")
		private Set<C> genericChildren = new HashSet<>();
	}

	@Entity
	public static class GenericCollectionEntity extends GenericCollectionMappedSuper<GenericCollectionChild> {
	}

	@Entity
	public static class GenericCollectionChild {
		@Id
		private Integer id;

		@ManyToOne
		private GenericCollectionEntity parent;
	}
}
