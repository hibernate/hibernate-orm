/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.attribute;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.annotations.AttributeBinderType;
import org.hibernate.annotations.Collate;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.Mutability;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.SharedSessionContract;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.models.bind.internal.model.AttributeBinding;
import org.hibernate.boot.models.bind.internal.model.ManagedTypeBinding;
import org.hibernate.boot.models.bind.internal.view.AttributeBindingView;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.orm.test.boot.models.bind.BindingTestingHelper;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.type.descriptor.java.MutabilityPlan;

import org.junit.jupiter.api.Test;

import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.ExcludedFromVersioning;
import jakarta.persistence.Id;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class AttributeBindingModelTests {
	@Test
	@ServiceRegistry
	void testOrdinaryAttributeBindings(ServiceRegistryScope scope) {
		CompatibilityBoundBinder.callCount.set( 0 );

		checkDomainModel(
				(context) -> {
					final EntityTypeMetadata entityType = entityType( context, AttributeBoundEntity.class );
					final ManagedTypeBinding managedTypeBinding = context.getBindingState()
							.getBootBindingModel()
							.getManagedTypeBinding( entityType.getClassDetails() );
					assertThat( managedTypeBinding ).isNotNull();

					final AttributeBindingView name = attribute( managedTypeBinding, "name" );
					assertThat( name.ownerType() ).isSameAs( managedTypeBinding );
					assertThat( name.declaringType() ).isSameAs( managedTypeBinding );
					assertThat( name.member().getName() ).isEqualTo( "name" );
					assertThat( name.accessType() ).isEqualTo( AccessType.FIELD );
					assertThat( name.nature() ).isEqualTo( AttributeNature.BASIC );
					assertThat( name.sourceRole() ).isEqualTo( AttributeBoundEntity.class.getName() + ".name" );
					assertThat( name.attributePath() ).isEqualTo( "name" );

					final AttributeBindingView code = attribute( managedTypeBinding, "code" );
					assertThat( code.isNaturalId() ).isTrue();
					assertThat( code.naturalIdMutable() ).isTrue();

					final AttributeBindingView collatedName = attribute( managedTypeBinding, "collatedName" );
					assertThat( collatedName.collation() ).isEqualTo( "ucs_basic" );

					final AttributeBindingView lazyName = attribute( managedTypeBinding, "lazyName" );
					assertThat( lazyName.lazyGroup() ).isEqualTo( "details" );

					final AttributeBindingView optimisticExcluded = attribute( managedTypeBinding, "optimisticExcluded" );
					assertThat( optimisticExcluded.optimisticLocked() ).isFalse();

					final AttributeBindingView versioningExcluded = attribute( managedTypeBinding, "versioningExcluded" );
					assertThat( versioningExcluded.optimisticLocked() ).isFalse();

					final AttributeBindingView immutableName = attribute( managedTypeBinding, "immutableName" );
					assertThat( immutableName.immutable() ).isTrue();

					final AttributeBindingView mutabilityPlanned = attribute( managedTypeBinding, "mutabilityPlanned" );
					assertThat( mutabilityPlanned.explicitMutabilityPlanClass() ).isEqualTo( CustomMutabilityPlan.class );

					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( AttributeBoundEntity.class.getName() );
					assertThat( entityBinding.getProperty( "code" ).isNaturalIdentifier() ).isTrue();
					assertThat( entityBinding.getProperty( "code" ).isUpdatable() ).isTrue();

					final BasicValue collatedValue = (BasicValue) entityBinding.getProperty( "collatedName" ).getValue();
					assertThat( ( (Column) collatedValue.getColumn() ).getCollation() ).isEqualTo( "ucs_basic" );

					assertThat( entityBinding.getProperty( "lazyName" ).getLazyGroup() ).isEqualTo( "details" );
					assertThat( entityBinding.getProperty( "optimisticExcluded" ).isOptimisticLocked() ).isFalse();
					assertThat( entityBinding.getProperty( "versioningExcluded" ).isOptimisticLocked() ).isFalse();
					assertThat( entityBinding.getProperty( "immutableName" ).isUpdatable() ).isFalse();

					final BasicValue mutabilityValue = (BasicValue) entityBinding.getProperty( "mutabilityPlanned" )
							.getValue();
					assertThat( mutabilityValue.getExplicitMutabilityPlanAccess() ).isNotNull();
					assertThat( mutabilityValue.getExplicitMutabilityPlanAccess()
							.apply( context.getMetadataCollector().getTypeConfiguration() ) )
							.isInstanceOf( CustomMutabilityPlan.class );

					final Property customBound = entityBinding.getProperty( "customBound" );
					assertThat( customBound.isUpdatable() ).isFalse();
					assertThat( CompatibilityBoundBinder.callCount ).hasValue( 1 );
				},
				scope.getRegistry(),
				AttributeBoundEntity.class
		);
	}

	private static AttributeBindingView attribute(ManagedTypeBinding managedTypeBinding, String name) {
		for ( AttributeBinding attributeBinding : managedTypeBinding.declaredAttributes() ) {
			if ( attributeBinding.attributeName().equals( name ) ) {
				return new AttributeBindingView( attributeBinding );
			}
		}
		throw new AssertionError( "Could not locate attribute binding " + name );
	}

	private static EntityTypeMetadata entityType(
			BindingTestingHelper.DomainModelCheckContext context,
			Class<?> entityClass) {
		for ( EntityHierarchy hierarchy : context.getCategorizedDomainModel().getEntityHierarchies() ) {
			if ( hierarchy.getRoot().getClassDetails().getClassName().equals( entityClass.getName() ) ) {
				return hierarchy.getRoot();
			}
		}
		throw new AssertionError( "Could not locate entity type for " + entityClass.getName() );
	}

	@Entity(name = "AttributeBoundEntity")
	public static class AttributeBoundEntity {
		@Id
		private Integer id;

		private String name;

		@NaturalId(mutable = true)
		private String code;

		@Collate("ucs_basic")
		private String collatedName;

		@LazyGroup("details")
		private String lazyName;

		@OptimisticLock(excluded = true)
		private String optimisticExcluded;

		@ExcludedFromVersioning
		private String versioningExcluded;

		@Immutable
		private String immutableName;

		@Mutability(CustomMutabilityPlan.class)
		private String mutabilityPlanned;

		@CompatibilityBound
		private String customBound;
	}

	public static class CustomMutabilityPlan implements MutabilityPlan<String> {
		@Override
		public boolean isMutable() {
			return true;
		}

		@Override
		public String deepCopy(String value) {
			return value;
		}

		@Override
		public Serializable disassemble(String value, SharedSessionContract session) {
			return value;
		}

		@Override
		public String assemble(Serializable cached, SharedSessionContract session) {
			return (String) cached;
		}
	}

	@Target(FIELD)
	@Retention(RUNTIME)
	@AttributeBinderType(binder = CompatibilityBoundBinder.class)
	public @interface CompatibilityBound {
	}

	public static class CompatibilityBoundBinder implements AttributeBinder<CompatibilityBound> {
		private static final AtomicInteger callCount = new AtomicInteger();

		@Override
		public void bind(
				CompatibilityBound annotation,
				MetadataBuildingContext buildingContext,
				PersistentClass persistentClass,
				Property property) {
			callCount.incrementAndGet();
			property.setUpdatable( false );
		}
	}
}
