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
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.Formula;
import org.hibernate.SharedSessionContract;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.mapping.internal.model.AttributeDeclarationBinding;
import org.hibernate.boot.mapping.internal.model.AttributeUsageBinding;
import org.hibernate.boot.mapping.internal.model.IdentifiableAttributeDeclarationBinding;
import org.hibernate.boot.mapping.internal.model.ManagedTypeBinding;
import org.hibernate.boot.mapping.internal.view.AttributeBindingView;
import org.hibernate.boot.mapping.internal.categorize.EntityHierarchy;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Selectable;
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
					assertThat( name.binding() ).isInstanceOf( AttributeUsageBinding.class );
					assertThat( name.declaration() ).isInstanceOf( AttributeDeclarationBinding.class );
					assertThat( name.declaration() ).isInstanceOf( IdentifiableAttributeDeclarationBinding.class );
					assertThat( name.declaration() ).isNotSameAs( name.usageBinding() );
					assertThat( managedTypeBinding.attributeUsages() ).contains( name.usageBinding() );
					assertThat( name.resolvedType().determineRawClass().toJavaClass() ).isEqualTo( String.class );

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

					final AttributeBindingView columnedName = attribute( managedTypeBinding, "columnedName" );
					assertThat( columnedName.basicValueIntent().columnSource().name() ).isEqualTo( "columned_name" );
					assertThat( columnedName.basicValueIntent().insertable() ).isFalse();
					assertThat( columnedName.basicValueIntent().updatable() ).isFalse();
					assertThat( columnedName.basicValueIntent().columnSource().nullable( true ) ).isFalse();
					assertThat( columnedName.basicValueIntent().columnSource().length( 255 ) ).isEqualTo( 64 );
					assertThat( columnedName.usageBinding().basicValueIntent() ).isSameAs( columnedName.basicValueIntent() );

					final AttributeBindingView formulaName = attribute( managedTypeBinding, "formulaName" );
					assertThat( formulaName.basicValueIntent().isFormula() ).isTrue();
					assertThat( formulaName.basicValueIntent().formulaExpression() ).isEqualTo( "upper(name)" );

					final AttributeBindingView transformedName = attribute( managedTypeBinding, "transformedName" );
					assertThat( transformedName.basicValueIntent().columnTransformerName() )
							.isEqualTo( "transformed_name" );
					assertThat( transformedName.basicValueIntent().customReadExpression() )
							.isEqualTo( "lower(transformed_name)" );
					assertThat( transformedName.basicValueIntent().customWriteExpression() )
							.isEqualTo( "upper(?)" );

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

					final Property columnedProperty = entityBinding.getProperty( "columnedName" );
					assertThat( columnedProperty.isInsertable() ).isFalse();
					assertThat( columnedProperty.isUpdatable() ).isFalse();
					final Column columnedColumn = (Column) ( (BasicValue) columnedProperty.getValue() ).getColumn();
					assertThat( columnedColumn.getName() ).isEqualTo( "columned_name" );
					assertThat( columnedColumn.isNullable() ).isFalse();
					assertThat( columnedColumn.getLength() ).isEqualTo( 64 );

					final Selectable formulaSelectable = ( (BasicValue) entityBinding.getProperty( "formulaName" )
							.getValue() ).getSelectables().get( 0 );
					assertThat( formulaSelectable.isFormula() ).isTrue();
					assertThat( formulaSelectable.getText() ).isEqualTo( "upper(name)" );

					final Column transformedColumn = (Column) ( (BasicValue) entityBinding.getProperty( "transformedName" )
							.getValue() ).getColumn();
					assertThat( transformedColumn.getName() ).isEqualTo( "transformed_name" );
					assertThat( transformedColumn.getCustomRead() ).isEqualTo( "lower(transformed_name)" );
					assertThat( transformedColumn.getCustomWrite() ).isEqualTo( "upper(?)" );

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
		for ( var attributeUsage : managedTypeBinding.attributeUsages() ) {
			if ( attributeUsage.attributeName().equals( name ) ) {
				return new AttributeBindingView( attributeUsage );
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

		@jakarta.persistence.Column(name = "columned_name", nullable = false, insertable = false, updatable = false, length = 64)
		private String columnedName;

		@Formula("upper(name)")
		private String formulaName;

		@jakarta.persistence.Column(name = "transformed_name")
		@ColumnTransformer(forColumn = "transformed_name", read = "lower(transformed_name)", write = "upper(?)")
		private String transformedName;

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
