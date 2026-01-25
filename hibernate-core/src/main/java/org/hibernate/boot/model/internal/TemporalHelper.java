/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.time.Instant;

import org.hibernate.annotations.Temporal;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.CheckConstraint;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Temporalized;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.TemporalMappingImpl;

/**
 * Helper for dealing with {@link org.hibernate.annotations.Temporal}.
 */
public class TemporalHelper {

	public static void bindTemporalColumns(
			Temporal temporalConfig,
			Temporalized target,
			Table table,
			MetadataBuildingContext context) {
		final Integer temporalPrecision =
				temporalConfig.secondPrecision() == -1 ? null : temporalConfig.secondPrecision();
		final var startingColumn =
				createTemporalColumn( temporalConfig.starting(), table, context, false, temporalPrecision );
		final var endingColumn =
				createTemporalColumn( temporalConfig.ending(), table, context, true, temporalPrecision );

		table.addColumn( startingColumn );
		table.addColumn( endingColumn );
		addTemporalCheckConstraint( table, startingColumn, endingColumn, context );
		target.enableTemporal( startingColumn, endingColumn );
	}

	public static TemporalMappingImpl resolveTemporalMapping(
			Temporalized bootMapping,
			String tableName,
			MappingModelCreationProcess creationProcess) {
		return bootMapping.isTemporalized()
				? new TemporalMappingImpl( bootMapping, tableName, creationProcess )
				: null;
	}

	private static Column createTemporalColumn(
			String columnName,
			Table table,
			MetadataBuildingContext context,
			boolean nullable,
			Integer temporalPrecision) {
		final var basicValue = new BasicValue( context, table );
		basicValue.setImplicitJavaTypeAccess( typeConfiguration -> Instant.class );

		final var column = new Column();
		column.setValue( basicValue );
		basicValue.addColumn( column );

		applyColumnName( column, columnName, context );
		column.setNullable( nullable );
		column.setTemporalPrecision( temporalPrecision );

		return column;
	}

	private static void applyColumnName(
			Column column,
			String columnName,
			MetadataBuildingContext context) {
		final var database = context.getMetadataCollector().getDatabase();
		final var namingStrategy = context.getBuildingOptions().getPhysicalNamingStrategy();
		final Identifier physicalColumnName = namingStrategy.toPhysicalColumnName(
				database.toIdentifier( columnName ),
				database.getJdbcEnvironment()
		);
		column.setName( physicalColumnName.render( database.getDialect() ) );
	}

	private static void addTemporalCheckConstraint(
			Table table,
			Column startingColumn,
			Column endingColumn,
			MetadataBuildingContext context) {
		final var dialect = context.getMetadataCollector().getDatabase().getDialect();
		final String starting = startingColumn.getQuotedName( dialect );
		final String ending = endingColumn.getQuotedName( dialect );
		table.addCheck( new CheckConstraint( ending + " is null or " + ending + " > " + starting ) );
	}
}
