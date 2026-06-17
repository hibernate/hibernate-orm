/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.mappedsuper;

import java.util.Set;

import org.hibernate.annotations.TenantId;
import org.hibernate.boot.models.categorize.spi.BasicKeyMapping;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
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
					final PersistentClass rootBinding = metadataCollector.getEntityBinding( RootConsumer.class.getName() );
					final PersistentClass leafBinding = metadataCollector.getEntityBinding( LeafConsumer.class.getName() );

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
					final PersistentClass appliedBinding = metadataCollector.getEntityBinding( EntityAfterContribution.class.getName() );
					final PersistentClass leafBinding = metadataCollector.getEntityBinding( EntityAfterContributionLeaf.class.getName() );

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
					final Property firstAppliedProperty = localProperty( firstRootBinding, "sharedCode" );
					final Property secondAppliedProperty = localProperty( secondRootBinding, "sharedCode" );

					assertThat( firstAppliedProperty ).isNotNull();
					assertThat( secondAppliedProperty ).isNotNull();
					assertThat( firstAppliedProperty ).isNotSameAs( secondAppliedProperty );
				},
				scope.getRegistry(),
				SharedContribution.class,
				FirstSharedRoot.class,
				SecondSharedRoot.class
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
}
