/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.mappedsuper;

import java.util.Set;

import org.hibernate.annotations.TenantId;
import org.hibernate.boot.mapping.internal.model.IdentifiableAttributeDeclarationBinding;
import org.hibernate.boot.mapping.internal.model.AttributeUsageBinding;
import org.hibernate.boot.mapping.internal.model.ManagedTypeBinding;
import org.hibernate.boot.mapping.internal.model.StandardAttributeUsageBinding;
import org.hibernate.boot.mapping.internal.categorize.BasicKeyMapping;
import org.hibernate.boot.mapping.internal.categorize.EntityHierarchy;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Subclass;
import org.hibernate.orm.test.boot.models.bind.callbacks.HierarchyRoot;
import org.hibernate.orm.test.boot.models.bind.callbacks.HierarchySuper;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;
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
					assertThat( rootBinding.getMappedSuperclassPropertyOrigin( rootCode ) ).isSameAs( contributionBinding );
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
					assertThat( appliedBinding.getMappedSuperclassPropertyOrigin( nestedCode ) ).isSameAs( contributionBinding );
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
					assertThat( firstRootBinding.getMappedSuperclassProperties() ).containsExactly( firstAppliedProperty );
					assertThat( secondRootBinding.getMappedSuperclassProperties() ).containsExactly( secondAppliedProperty );
					assertThat( firstRootBinding.getMappedSuperclassPropertyOrigin( firstAppliedProperty ) )
							.isSameAs( firstContribution );
					assertThat( secondRootBinding.getMappedSuperclassPropertyOrigin( secondAppliedProperty ) )
							.isSameAs( secondContribution );
				},
				scope.getRegistry(),
				SharedContribution.class,
				FirstSharedRoot.class,
				SecondSharedRoot.class
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

					assertThat( usage ).isInstanceOf( StandardAttributeUsageBinding.class );
					assertThat( usage.declaration() ).isInstanceOf( IdentifiableAttributeDeclarationBinding.class );
					assertThat( usage.declaration() ).isSameAs( superBinding.declaredAttributes().get( 0 ) );
					assertThat( usage.resolvedType().determineRawClass().toJavaClass() ).isEqualTo( String.class );
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

	@jakarta.persistence.MappedSuperclass
	public static class GenericMappedSuper<T> {
		@Id
		private Integer id;
		private T genericValue;
	}

	@Entity
	public static class GenericStringEntity extends GenericMappedSuper<String> {
	}
}
