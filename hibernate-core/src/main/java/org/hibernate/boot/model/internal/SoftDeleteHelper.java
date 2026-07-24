/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.boot.mapping.internal.context.MappingResolutionState;
import org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionBuilder;
import org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionDetails;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;
import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SoftDeletable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.MappingRole;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.SoftDeletableModelPart;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.SoftDeleteMappingImpl;

import java.time.Instant;

import static org.hibernate.internal.util.StringHelper.coalesce;
import static org.hibernate.internal.util.StringHelper.isBlank;

/**
 * Helper for dealing with {@link org.hibernate.annotations.SoftDelete}
 *
 * @author Steve Ebersole
 */
public class SoftDeleteHelper {
	/**
	 * Creates and binds the column and value for modeling the soft-delete in the database
	 *
	 * @param softDeleteConfig The SoftDelete annotation
	 * @param target The thing which is to be soft-deleted
	 * @param table The table to which the soft-delete should be applied
	 * @param context The processing context for access to needed info and services
	 */
	public static void bindSoftDeleteIndicator(
			SoftDelete softDeleteConfig,
			SoftDeletable target,
			Table table,
			MetadataBuildingContext context) {
		assert softDeleteConfig != null;
		final BasicValue softDeleteIndicatorValue = createSoftDeleteIndicatorValue( softDeleteConfig, table, context );
		assignMappingRole( target, softDeleteIndicatorValue );
		final var softDeleteIndicatorColumn =
				createSoftDeleteIndicatorColumn( softDeleteConfig, softDeleteIndicatorValue, context );
		applyResolution( softDeleteConfig, softDeleteIndicatorColumn, context );
		table.addColumn( softDeleteIndicatorColumn );
		target.enableSoftDelete( softDeleteIndicatorColumn, softDeleteConfig.strategy() );
	}

	private static void assignMappingRole(SoftDeletable target, BasicValue value) {
		if ( target instanceof Collection collection && collection.getRole() != null ) {
			value.setMappingRole(
					MappingRole.collection( collection.getRole() ).append( MappingRole.PartKind.SOFT_DELETE )
			);
		}
		else if ( target instanceof RootClass rootClass && rootClass.getEntityName() != null ) {
			value.setMappingRole(
					MappingRole.entity( rootClass.getEntityName() ).append( MappingRole.PartKind.SOFT_DELETE )
			);
		}
	}

	private static BasicValue createSoftDeleteIndicatorValue(
			SoftDelete softDeleteConfig,
			Table table,
			MetadataBuildingContext context) {
		final var softDeleteIndicatorValue = BasicValue.unregistered( context, table );
		softDeleteIndicatorValue.makeSoftDelete( softDeleteConfig.strategy() );

		if ( softDeleteConfig.strategy() == SoftDeleteType.TIMESTAMP ) {
			if ( softDeleteConfig.converter() != SoftDelete.UnspecifiedConversion.class ) {
				throw new UnsupportedMappingException(
						"Specifying SoftDelete#converter in conjunction with SoftDeleteType.TIMESTAMP is not supported"
				);
			}
		}
		else {
			final ConverterDescriptor<Boolean,?> converterDescriptor =
					ConverterDescriptors.of( softDeleteConfig.converter() );
			softDeleteIndicatorValue.setJpaAttributeConverterDescriptor( converterDescriptor );
		}

		return softDeleteIndicatorValue;
	}

	private static void applyResolution(
			SoftDelete softDeleteConfig,
			Column softDeleteIndicatorColumn,
			MetadataBuildingContext context) {
		final var softDeleteIndicatorValue = (BasicValue) softDeleteIndicatorColumn.getValue();
		final var resolutionInput = BasicValueResolutionDetails.create(
				softDeleteIndicatorValue,
				BasicValueSource.softDelete()
		);
		resolutionInput.setResolvedJavaType(
				softDeleteConfig.strategy() == SoftDeleteType.TIMESTAMP
						? Instant.class
						: softDeleteIndicatorValue.getJpaAttributeConverterDescriptor().getRelationalValueResolvedType()
		);
		BasicValueResolutionBuilder.applyResolution(
				resolutionInput,
				context.getServiceComponents(),
				MappingResolutionState.from( context ),
				context
		);
	}

	private static Column createSoftDeleteIndicatorColumn(
			SoftDelete softDeleteConfig,
			BasicValue softDeleteIndicatorValue,
			MetadataBuildingContext context) {
		final var softDeleteColumn = new Column();

		softDeleteColumn.setValue( softDeleteIndicatorValue );
		softDeleteIndicatorValue.addColumn( softDeleteColumn );

		applyColumnName( softDeleteColumn, softDeleteConfig, context );

		softDeleteColumn.setOptions( softDeleteConfig.options() );
		if ( isBlank( softDeleteConfig.comment() ) ) {
			softDeleteColumn.setComment( "Soft-delete indicator" );
		}
		else {
			softDeleteColumn.setComment( softDeleteConfig.comment() );
		}

		softDeleteColumn.setUnique( false );

		if ( softDeleteConfig.strategy() == SoftDeleteType.TIMESTAMP ) {
			softDeleteColumn.setNullable( true );
		}
		else {
			softDeleteColumn.setLength( 1 );
			softDeleteColumn.setNullable( false );
		}

		return softDeleteColumn;
	}

	private static void applyColumnName(
			Column softDeleteColumn,
			SoftDelete softDeleteConfig,
			MetadataBuildingContext context) {
		final var database = context.getMetadataCollector().getDatabase();
		final var namingStrategy = context.getBuildingPlan().getPhysicalNamingStrategy();
		// NOTE: the argument order is strange here - the fallback value comes first
		final String logicalColumnName = coalesce(
				softDeleteConfig.strategy().getDefaultColumnName(),
				softDeleteConfig.columnName()
		);
		final Identifier physicalColumnName = namingStrategy.toPhysicalColumnName(
				database.toIdentifier( logicalColumnName ),
				database.getJdbcEnvironment()
		);
		softDeleteColumn.setName( physicalColumnName.render( database.getDialect() ) );
	}

	public static SoftDeleteMappingImpl resolveSoftDeleteMapping(
			SoftDeletableModelPart softDeletableModelPart,
			SoftDeletable bootMapping,
			String tableName,
			MappingModelCreationProcess creationProcess) {
		return bootMapping.getSoftDeleteColumn() == null
				? null
				: new SoftDeleteMappingImpl( softDeletableModelPart, bootMapping, tableName, creationProcess );
	}

}
