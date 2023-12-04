/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import org.hibernate.MappingException;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.models.bind.internal.binders.ColumnBinder;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.boot.models.categorize.spi.CacheRegion;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.MappedSuperclassTypeMetadata;
import org.hibernate.boot.models.categorize.spi.NaturalIdCacheRegion;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.AnnotationUsage;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.InheritanceType;

import static org.hibernate.boot.models.bind.ModelBindingLogging.MODEL_BINDING_LOGGER;
import static org.hibernate.boot.models.bind.internal.binders.TenantIdBinder.bindTenantId;
import static org.hibernate.boot.models.bind.internal.binders.VersionBinder.bindVersion;
import static org.hibernate.internal.util.StringHelper.coalesce;

/**
 * Binding for a root {@linkplain jakarta.persistence.metamodel.EntityType entity type}
 *
 * @author Steve Ebersole
 */
public class RootEntityBinding extends EntityBinding {
	private final RootClass rootClass;
	private final TableReference tableReference;
	private final IdentifierBinding identifierBinding;

	public RootEntityBinding(
			EntityTypeMetadata typeMetadata,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		super(
				typeMetadata,
				resolveSuperTypeBinding(
						(MappedSuperclassTypeMetadata) typeMetadata.getSuperType(),
						bindingOptions,
						bindingState,
						bindingContext
				),
				EntityHierarchy.HierarchyRelation.ROOT,
				bindingOptions,
				bindingState,
				bindingContext
		);

		this.rootClass = new RootClass( bindingState.getMetadataBuildingContext() );
		applyNaming( typeMetadata, rootClass, bindingState );

		this.tableReference = TableHelper.bindPrimaryTable(
				typeMetadata,
				EntityHierarchy.HierarchyRelation.ROOT,
				bindingOptions,
				bindingState,
				bindingContext
		);

		processSecondaryTables();

		bindingState.registerTypeBinding( typeMetadata, this );

		this.rootClass.setTable( tableReference.table() );

		if ( getSuperTypeBinding() != null ) {
			rootClass.setSuperMappedSuperclass( (MappedSuperclass) getSuperTypeBinding().getBinding() );
		}

		this.identifierBinding = new IdentifierBinding( this, typeMetadata, bindingOptions, bindingState, bindingContext );
		this.rootClass.setIdentifier( identifierBinding.getValue() );

		if ( typeMetadata.hasSubTypes() ) {
			bindDiscriminator( typeMetadata, rootClass, bindingOptions, bindingState, bindingContext );
			applyDiscriminatorValue( typeMetadata, rootClass );
		}

		bindVersion( typeMetadata, rootClass, bindingOptions, bindingState, bindingContext );
		bindTenantId( typeMetadata, rootClass, bindingOptions, bindingState, bindingContext );
		applyOptimisticLocking( typeMetadata, rootClass );
		applyCacheRegions( typeMetadata, rootClass );
		applySoftDelete( typeMetadata, rootClass, tableReference.table() );

		applyCaching( typeMetadata, rootClass, bindingState );
		applyFilters( typeMetadata, rootClass );
		applyJpaEventListeners( typeMetadata, rootClass );

		// todo : handle any super mapped-superclasses

		prepareAttributeBindings( tableReference.table() );

		prepareSubclassBindings();
	}

	private static IdentifiableTypeBinding resolveSuperTypeBinding(
			MappedSuperclassTypeMetadata superTypeMetadata,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( superTypeMetadata == null ) {
			return null;
		}

		final IdentifiableTypeBinding superTypeSuperTypeBinding;
		if ( superTypeMetadata.getSuperType() == null ) {
			superTypeSuperTypeBinding = null;
		}
		else {
			superTypeSuperTypeBinding = resolveSuperTypeBinding(
					(MappedSuperclassTypeMetadata) superTypeMetadata.getSuperType(),
					bindingOptions,
					bindingState,
					bindingContext
			);
		}

		return new MappedSuperclassBinding(
				superTypeMetadata,
				superTypeSuperTypeBinding,
				EntityHierarchy.HierarchyRelation.SUPER,
				bindingOptions,
				bindingState,
				bindingContext
		);
	}

	@Override
	public RootClass getPersistentClass() {
		return rootClass;
	}

	@Override
	public RootClass getBinding() {
		return getPersistentClass();
	}

	public TableReference getTableReference() {
		return tableReference;
	}

	public IdentifierBinding getIdentifierBinding() {
		return identifierBinding;
	}

	private void applyCacheRegions(EntityTypeMetadata source, RootClass rootClass) {
		final EntityHierarchy hierarchy = source.getHierarchy();
		final CacheRegion cacheRegion = hierarchy.getCacheRegion();
		final NaturalIdCacheRegion naturalIdCacheRegion = hierarchy.getNaturalIdCacheRegion();

		if ( cacheRegion != null ) {
			rootClass.setCacheRegionName( cacheRegion.getRegionName() );
			rootClass.setCacheConcurrencyStrategy( cacheRegion.getAccessType().getExternalName() );
			rootClass.setLazyPropertiesCacheable( cacheRegion.isCacheLazyProperties() );
		}

		if ( naturalIdCacheRegion != null ) {
			rootClass.setNaturalIdCacheRegionName( naturalIdCacheRegion.getRegionName() );
		}
	}

	private void applyOptimisticLocking(EntityTypeMetadata source, RootClass rootClass) {
		final var classDetails = source.getClassDetails();
		final var optimisticLocking = classDetails.getAnnotationUsage( OptimisticLocking.class );

		if ( optimisticLocking != null ) {
			final var optimisticLockingType = optimisticLocking.getEnum( "type", OptimisticLockType.VERSION );
			rootClass.setOptimisticLockStyle( OptimisticLockStyle.valueOf( optimisticLockingType.name() ) );
		}
	}

	private void applySoftDelete(EntityTypeMetadata source, RootClass rootClass, Table primaryTable) {
		final var classDetails = source.getClassDetails();
		final AnnotationUsage<SoftDelete> softDeleteConfig = classDetails.getAnnotationUsage( SoftDelete.class );
		if ( softDeleteConfig == null ) {
			return;
		}

		final BasicValue softDeleteIndicatorValue = createSoftDeleteIndicatorValue( softDeleteConfig, primaryTable );
		final Column softDeleteIndicatorColumn = createSoftDeleteIndicatorColumn( softDeleteConfig, softDeleteIndicatorValue );
		primaryTable.addColumn( softDeleteIndicatorColumn );
		rootClass.enableSoftDelete( softDeleteIndicatorColumn );
	}

	private BasicValue createSoftDeleteIndicatorValue(
			AnnotationUsage<SoftDelete> softDeleteAnn,
			Table table) {
		assert softDeleteAnn != null;

		final var converterClassDetails = softDeleteAnn.getClassDetails( "converter" );
		final ClassBasedConverterDescriptor converterDescriptor = new ClassBasedConverterDescriptor(
				converterClassDetails.toJavaClass(),
				bindingContext.getBootstrapContext().getClassmateContext()
		);

		final BasicValue softDeleteIndicatorValue = new BasicValue( bindingState.getMetadataBuildingContext(), table );
		softDeleteIndicatorValue.makeSoftDelete( softDeleteAnn.getEnum( "strategy" ) );
		softDeleteIndicatorValue.setJpaAttributeConverterDescriptor( converterDescriptor );
		softDeleteIndicatorValue.setImplicitJavaTypeAccess( (typeConfiguration) -> converterDescriptor.getRelationalValueResolvedType().getErasedType() );
		return softDeleteIndicatorValue;
	}

	private Column createSoftDeleteIndicatorColumn(
			AnnotationUsage<SoftDelete> softDeleteConfig,
			BasicValue softDeleteIndicatorValue) {
		final Column softDeleteColumn = new Column();

		applyColumnName( softDeleteColumn, softDeleteConfig );

		softDeleteColumn.setLength( 1 );
		softDeleteColumn.setNullable( false );
		softDeleteColumn.setUnique( false );
		softDeleteColumn.setComment( "Soft-delete indicator" );

		softDeleteColumn.setValue( softDeleteIndicatorValue );
		softDeleteIndicatorValue.addColumn( softDeleteColumn );

		return softDeleteColumn;
	}

	private void applyColumnName(Column softDeleteColumn, AnnotationUsage<SoftDelete> softDeleteConfig) {
		final Database database = bindingState.getMetadataBuildingContext().getMetadataCollector().getDatabase();
		final PhysicalNamingStrategy namingStrategy = bindingState.getMetadataBuildingContext().getBuildingOptions().getPhysicalNamingStrategy();
		final SoftDeleteType strategy = softDeleteConfig.getEnum( "strategy" );
		final String logicalColumnName = coalesce(
				strategy.getDefaultColumnName(),
				softDeleteConfig.getString( "columnName" )
		);
		final Identifier physicalColumnName = namingStrategy.toPhysicalColumnName(
				database.toIdentifier( logicalColumnName ),
				database.getJdbcEnvironment()
		);
		softDeleteColumn.setName( physicalColumnName.render( database.getDialect() ) );
	}
	public static void bindDiscriminator(
			EntityTypeMetadata typeMetadata,
			RootClass rootClass,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final InheritanceType inheritanceType = typeMetadata.getHierarchy().getInheritanceType();
		final AnnotationUsage<DiscriminatorColumn> columnAnn = typeMetadata.getClassDetails().getAnnotationUsage( DiscriminatorColumn.class );
		final AnnotationUsage<DiscriminatorFormula> formulaAnn = typeMetadata.getClassDetails().getAnnotationUsage( DiscriminatorFormula.class );

		if ( columnAnn != null && formulaAnn != null ) {
			throw new MappingException( "Entity defined both @DiscriminatorColumn and @DiscriminatorFormula - " + typeMetadata.getEntityName() );
		}

		if ( inheritanceType == InheritanceType.TABLE_PER_CLASS ) {
			// UnionSubclass cannot define a discriminator
			return;
		}

		if ( inheritanceType == InheritanceType.JOINED ) {
			// JoinedSubclass can define a discriminator in certain circumstances
			final MetadataBuildingOptions buildingOptions = bindingState.getMetadataBuildingContext().getBuildingOptions();

			if ( buildingOptions.ignoreExplicitDiscriminatorsForJoinedInheritance() ) {
				if ( columnAnn != null || formulaAnn != null ) {
					MODEL_BINDING_LOGGER.debugf( "Skipping explicit discriminator for JOINED hierarchy due to configuration - " + rootClass.getEntityName() );
				}
				return;
			}

			if ( !buildingOptions.createImplicitDiscriminatorsForJoinedInheritance() ) {
				if ( columnAnn == null && formulaAnn == null ) {
					return;
				}
			}
		}

		if ( inheritanceType == InheritanceType.SINGLE_TABLE ) {
			if ( !typeMetadata.hasSubTypes() ) {
				return;
			}
		}

		final BasicValue value = new BasicValue( bindingState.getMetadataBuildingContext(), rootClass.getIdentityTable() );
		rootClass.setDiscriminator( value );

		final DiscriminatorType discriminatorType = ColumnBinder.bindDiscriminatorColumn(
				bindingContext,
				formulaAnn,
				value,
				columnAnn,
				bindingOptions,
				bindingState
		);

		final Class<?> discriminatorJavaType;
		switch ( discriminatorType ) {
			case STRING -> discriminatorJavaType = String.class;
			case CHAR -> discriminatorJavaType = char.class;
			case INTEGER -> discriminatorJavaType = int.class;
			default -> throw new IllegalStateException( "Unexpected DiscriminatorType - " + discriminatorType );
		}

		value.setImplicitJavaTypeAccess( typeConfiguration -> discriminatorJavaType );
	}
}
