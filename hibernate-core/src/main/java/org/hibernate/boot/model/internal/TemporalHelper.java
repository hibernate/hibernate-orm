/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.time.Instant;
import java.util.Map;

import org.hibernate.annotations.Temporal;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.TemporalTableStrategy;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.CheckConstraint;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Temporalized;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.TemporalMappingImpl;

import static org.hibernate.cfg.MappingSettings.TEMPORAL_TABLE_STRATEGY;
import static org.hibernate.cfg.TemporalTableStrategy.NATIVE;
import static org.hibernate.cfg.TemporalTableStrategy.VM_TIMESTAMP;

/**
 * Helper for dealing with {@link org.hibernate.annotations.Temporal}.
 */
public class TemporalHelper {

	public static void bindTemporalColumns(
			Temporal temporal,
			Temporalized target,
			Table table,
			MetadataBuildingContext context) {
		final int secondPrecision = temporal.secondPrecision();
		final Integer precision = secondPrecision == -1 ? null : secondPrecision;
		final var startingColumn =
				createTemporalColumn( temporal.rowStart(),
						table, false, precision, context );
		final var endingColumn =
				createTemporalColumn( temporal.rowEnd(),
						table, true, precision, context );

		if ( usingNativeTemporalTables( context ) ) {
			applyNativeTemporalTableOptions( startingColumn, endingColumn );
//			createTransactionIdColumn( table, precision, context );
		}

		final boolean partitioned = temporal.partitioned();
		table.addColumn( startingColumn );
		table.addColumn( endingColumn );
		target.enableTemporal( startingColumn, endingColumn, partitioned );

		addExtraDeclarationsAndOptions( table, startingColumn, endingColumn, partitioned, context );
		addTemporalCheckConstraint( table, startingColumn, endingColumn, context );
		addAuxiliaryObjects( table, partitioned, context );
	}

	private static void addExtraDeclarationsAndOptions(
			Table table,
			Column startingColumn, Column endingColumn,
			boolean partitioned,
			MetadataBuildingContext context) {
		final var dialect = context.getMetadataCollector().getDatabase().getDialect();
		TemporalTableStrategy strategy = getTemporalTableStrategy( context );
		table.setExtraDeclarations( dialect.getExtraTemporalTableDeclarations(
				strategy, startingColumn.getQuotedName( dialect ),
				endingColumn.getQuotedName( dialect ),
				partitioned
		) );
		table.setOptions( appendOption( table.getOptions(),
				dialect.getTemporalTableOptions(
						strategy,
						endingColumn.getQuotedName( dialect ),
						partitioned
				) ) );
	}

	private static void addAuxiliaryObjects(
			Table table,
			boolean partitioned,
			MetadataBuildingContext context) {
		final var database = context.getMetadataCollector().getDatabase();
		database.getDialect()
				.addTemporalTableAuxiliaryObjects( getTemporalTableStrategy( context ), table, database,
						partitioned );
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
			boolean nullable,
			Integer temporalPrecision,
			MetadataBuildingContext context) {
		final var basicValue = new BasicValue( context, table );
		basicValue.setImplicitJavaTypeAccess( typeConfiguration -> Instant.class );
		final var column = new Column();
		column.setNullable( nullable );
		column.setValue( basicValue );
		basicValue.addColumn( column );
		final var database = context.getMetadataCollector().getDatabase();
		setTemporalColumnName( columnName, column, database,
				context.getBuildingOptions().getPhysicalNamingStrategy() );
		setTemporalColumnType( temporalPrecision, column, database );
		return column;
	}

	private static void setTemporalColumnType(
			Integer temporalPrecision,
			Column column,
			Database database) {
		if ( temporalPrecision != null ) {
			column.setTemporalPrecision( temporalPrecision );
		}
		else {
			final var dialect = database.getDialect();
			column.setTemporalPrecision( dialect.getTemporalColumnPrecision() );
			column.setSqlTypeCode( dialect.getTemporalColumnType() );
		}
	}

	private static void setTemporalColumnName(
			String name,
			Column column,
			Database database,
			PhysicalNamingStrategy physicalNamingStrategy) {
		final Identifier physicalColumnName =
						physicalNamingStrategy.toPhysicalColumnName(
								database.toIdentifier( name ),
								database.getJdbcEnvironment()
						);
		column.setName( physicalColumnName.render( database.getDialect() ) );
	}

	private static void addTemporalCheckConstraint(
			Table table,
			Column startingColumn,
			Column endingColumn,
			MetadataBuildingContext context) {
		if ( !usingNativeTemporalTables( context ) ) {
			final var dialect = context.getMetadataCollector().getDatabase().getDialect();
			final String starting = startingColumn.getQuotedName( dialect );
			final String ending = endingColumn.getQuotedName( dialect );
			table.addCheck( new CheckConstraint( ending + " is null or " + ending + " > " + starting ) );
		}
	}

	public static boolean usingNativeTemporalTables(MetadataBuildingContext context) {
		return getTemporalTableStrategy( context ) == NATIVE
			&& context.getMetadataCollector().getDatabase().getDialect()
					.supportsNativeTemporalTables();
	}

	public static boolean suppressesTemporalTablePrimaryKeys(boolean partitioned, MetadataBuildingContext context) {
		return context.getMetadataCollector().getDatabase().getDialect()
				.suppressesTemporalTablePrimaryKeys( partitioned );
	}

	private static TemporalTableStrategy getTemporalTableStrategy(MetadataBuildingContext context) {
		final var settings =
				context.getBootstrapContext().getServiceRegistry()
						.requireService( ConfigurationService.class )
						.getSettings();
		return determineTemporalTableStrategy( settings );
	}

	private static void applyNativeTemporalTableOptions(Column startingColumn, Column endingColumn) {
		startingColumn.setGeneratedAs( "row start" );
		endingColumn.setGeneratedAs( "row end" );
	}

	private static String appendOption(String existing, String option) {
		if ( existing == null || existing.isBlank() ) {
			return option;
		}
		if ( option == null || option.isBlank() ) {
			return existing;
		}
		else {
			return existing + " " + option;
		}
	}

	public static TemporalTableStrategy determineTemporalTableStrategy(
			Map<String, Object> configurationSettings) {
		final Object setting = configurationSettings.get( TEMPORAL_TABLE_STRATEGY );
		if ( setting instanceof TemporalTableStrategy temporalTableStrategy ) {
			return temporalTableStrategy;
		}
		else if ( setting instanceof String string ) {
			for ( var strategy : TemporalTableStrategy.values() ) {
				if ( strategy.name().equalsIgnoreCase( string ) ) {
					return strategy;
				}
			}
			return VM_TIMESTAMP;
		}
		else {
			return VM_TIMESTAMP;
		}
	}
}
