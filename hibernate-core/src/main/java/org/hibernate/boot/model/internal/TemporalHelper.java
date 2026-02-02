/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.Temporal;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.TemporalTableStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.CheckConstraint;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Stateful;
import org.hibernate.mapping.Table;
import org.hibernate.persister.state.internal.HistoryStateManagement;
import org.hibernate.persister.state.internal.NativeTemporalStateManagement;
import org.hibernate.persister.state.internal.TemporalStateManagement;
import org.hibernate.service.TransactionIdentifierService;

import static org.hibernate.cfg.MappingSettings.TEMPORAL_TABLE_STRATEGY;
import static org.hibernate.cfg.TemporalTableStrategy.AUTO;
import static org.hibernate.cfg.TemporalTableStrategy.NATIVE;
import static org.hibernate.cfg.TemporalTableStrategy.SINGLE_TABLE;
import static org.hibernate.internal.util.StringHelper.isBlank;

/**
 * Helper for dealing with {@link org.hibernate.annotations.Temporal}.
 */
public class TemporalHelper {

	public static final String ROW_START = "rowStart";
	public static final String ROW_END = "rowEnd";

	public static void bindTemporalColumns(
			Temporal temporal,
			Stateful target,
			Table table,
			Temporal.HistoryTable historyTable,
			Temporal.HistoryPartitioning historyPartitioning,
			MetadataBuildingContext context) {
		final var collector = context.getMetadataCollector();
		final boolean partitioned = historyPartitioning != null;
		final String currentPartitionName =
				partitioned
						? inferredPartitionName( historyPartitioning.currentPartition(),
								"current", table, collector )
						: null;
		final String historyPartitionName =
				partitioned
						? inferredPartitionName( historyPartitioning.historyPartition(),
								"history", table, collector )
						: null;

		final int secondPrecision = temporal.secondPrecision();
		final Integer precision = secondPrecision == -1 ? null : secondPrecision;
		final var transactionIdType = getTransactionIdType( context );
		final var rowStartColumn =
				createTemporalColumn( temporal.rowStart(),
						table, false, precision, transactionIdType, context );
		final var rowEndColumn =
				createTemporalColumn( temporal.rowEnd(),
						table, true, precision, transactionIdType, context );
		handleTemporalColumnGeneration( rowStartColumn, rowEndColumn, context );

		final var temporalTable =
				context.getTemporalTableStrategy() == TemporalTableStrategy.HISTORY_TABLE
						? createHistoryTable( table, historyTable, context )
						: table;
		temporalTable.addColumn( rowStartColumn );
		temporalTable.addColumn( rowEndColumn );
		enableTemporal( target, context.getTemporalTableStrategy(),
				rowStartColumn, rowEndColumn, temporalTable, partitioned,
				collector.getDatabase().getDialect() );

		addExtraDeclarationsAndOptions(
				temporalTable,
				rowStartColumn,
				rowEndColumn,
				partitioned,
				currentPartitionName,
				historyPartitionName,
				context
		);
		addTemporalCheckConstraint( temporalTable, rowStartColumn, rowEndColumn, context );
		addAuxiliaryObjects( temporalTable, partitioned, currentPartitionName, historyPartitionName, context );
		addSecondPass( target, context );
	}

	static void enableTemporal(
			Stateful model,
			TemporalTableStrategy temporalTableStrategy,
			Column rowStartColumn, Column rowEndColumn,
			Table temporalTable, boolean partitioned,
			Dialect dialect) {
		model.addAuxiliaryColumn( ROW_START, rowStartColumn );
		model.addAuxiliaryColumn( ROW_END, rowEndColumn );
		model.setAuxiliaryTable( temporalTable );
		model.setMainTablePartitioned( partitioned );
		model.setStateManagementType( switch ( temporalTableStrategy ) {
			case NATIVE -> NativeTemporalStateManagement.class;
			case SINGLE_TABLE -> TemporalStateManagement.class;
			case HISTORY_TABLE -> HistoryStateManagement.class;
			case AUTO -> throw new IllegalArgumentException();
		} );
		if ( temporalTableStrategy == SINGLE_TABLE ) {
			model.setAuxiliaryColumnInPrimaryKey( ROW_START );
		}
		model.setPrimaryKeyDisabled( dialect.getTemporalTableSupport()
				.suppressesTemporalTablePrimaryKeys( partitioned ) );
	}

	private static Class<?> getTransactionIdType(MetadataBuildingContext context) {
		return context.getBootstrapContext().getServiceRegistry()
				.requireService( TransactionIdentifierService.class )
				.getIdentifierType();
	}

	private static String inferredPartitionName(
			String partitionName, String suffix, Table table, InFlightMetadataCollector collector) {
		return partitionName == null || partitionName.isBlank()
				? collector.getLogicalTableName( table ) + '_' + suffix
				: partitionName;
	}

	private static void addSecondPass(Stateful target, MetadataBuildingContext context) {
		if ( context.getTemporalTableStrategy() == TemporalTableStrategy.HISTORY_TABLE ) {
			context.getMetadataCollector().addSecondPass( (OptionalDeterminationSecondPass) ignored -> {
				copyTableColumns( target.getMainTable(), target.getAuxiliaryTable() );
				createHistoryTablePrimaryKey( target,
						context.getMetadataCollector().getDatabase().getDialect() );
			} );
		}
	}

	private static Table createHistoryTable(
			Table table,
			Temporal.HistoryTable temporalHistoryTable,
			MetadataBuildingContext context) {
		final var collector = context.getMetadataCollector();
		final String explicitHistoryTableName =
				temporalHistoryTable == null ? null : temporalHistoryTable.name();
		final boolean hasExplicitHistoryTableName = !isBlank( explicitHistoryTableName );
		final String historyTableName =
				hasExplicitHistoryTableName
						? explicitHistoryTableName
						: collector.getLogicalTableName( table )
								+ "_history";
		final boolean explicitHistoryName =
				hasExplicitHistoryTableName
						|| table.getNameIdentifier().isExplicit();
		final var historyTable = collector.addTable(
				table.getSchema(),
				table.getCatalog(),
				historyTableName,
				table.getSubselect(),
				table.isAbstract(),
				context,
				explicitHistoryName
		);
		collector.addTableNameBinding( table.getNameIdentifier(), historyTable );
		copyTableColumns( table, historyTable );
		return historyTable;
	}

	private static void copyTableColumns(Table sourceTable, Table targetTable) {
		for ( var column : sourceTable.getColumns() ) {
			targetTable.addColumn( column.clone() );
		}
	}

	private static void createHistoryTablePrimaryKey(
			Stateful target,
			Dialect dialect) {
		final var historyTable = target.getAuxiliaryTable();
		final var startingColumn = target.getAuxiliaryColumn( ROW_START );
		if ( target instanceof RootClass rootClass ) {
			final var primaryKey = new PrimaryKey( historyTable );
			addColumnsByName( primaryKey, historyTable,
					rootClass.getKey().getColumns() );
			if ( dialect.addPartitionKeyToPrimaryKey() ) {
				for ( var property : rootClass.getProperties() ) {
					if ( property.getValue().isPartitionKey() ) {
						addColumnsByName( primaryKey, historyTable,
								property.getValue().getColumns() );
					}
				}
			}
			if ( primaryKey.getColumnSpan() > 0 ) {
				if ( rootClass.isVersioned() ) {
					final var versionProperty = rootClass.getVersion();
					if ( versionProperty != null ) {
						addColumnsByName( primaryKey, historyTable,
								versionProperty.getValue().getColumns() );
					}
				}
				else {
					primaryKey.addColumn( startingColumn );
				}
				historyTable.setPrimaryKey( primaryKey );
			}
		}
		else if ( target instanceof Collection collection ) {
			final var table = collection.getCollectionTable();
			final var sourcePrimaryKey = table == null ? null : table.getPrimaryKey();
			if ( sourcePrimaryKey != null ) {
				final var primaryKey = new PrimaryKey( historyTable );
				addColumnsByName( primaryKey, historyTable,
						sourcePrimaryKey.getColumns() );
				if ( primaryKey.getColumnSpan() > 0 ) {
					if ( !primaryKey.containsColumn( startingColumn ) ) {
						primaryKey.addColumn( startingColumn );
					}
					historyTable.setPrimaryKey( primaryKey );
				}
			}
		}
	}

	private static void addColumnsByName(PrimaryKey primaryKey, Table table, List<Column> columns) {
		for ( var column : columns ) {
			final var historyColumn = table.getColumn( column );
			if ( historyColumn != null
					&& !primaryKey.containsColumn( historyColumn ) ) {
				primaryKey.addColumn( historyColumn );
			}
		}
	}

	private static void addExtraDeclarationsAndOptions(
			Table table,
			Column startingColumn, Column rowEndColumn,
			boolean partitioned,
			String currentPartitionName,
			String historyPartitionName,
			MetadataBuildingContext context) {
		final var dialect = context.getMetadataCollector().getDatabase().getDialect();
		final var strategy = context.getTemporalTableStrategy();
		final var temporalTableSupport = dialect.getTemporalTableSupport();
		table.setExtraDeclarations( temporalTableSupport.getExtraTemporalTableDeclarations(
				strategy,
				startingColumn.getQuotedName( dialect ),
				rowEndColumn.getQuotedName( dialect ),
				partitioned
		) );
		String rowEndColumnName = rowEndColumn.getQuotedName( dialect );
		table.setOptions( appendOption( table.getOptions(),
				temporalTableSupport.getTemporalTableOptions(
						strategy,
						rowEndColumnName,
						partitioned,
						currentPartitionName,
						historyPartitionName
				) ) );
	}

	private static void addAuxiliaryObjects(
			Table table,
			boolean partitioned,
			String currentPartitionName,
			String historyPartitionName,
			MetadataBuildingContext context) {
		final var database = context.getMetadataCollector().getDatabase();
		database.getDialect().getTemporalTableSupport()
				.addTemporalTableAuxiliaryObjects(
						context.getTemporalTableStrategy(),
						table, database,
						partitioned,
						currentPartitionName,
						historyPartitionName
				);
	}

	private static Column createTemporalColumn(
			String columnName,
			Table table,
			boolean nullable,
			Integer temporalPrecision,
			Class<?> transactionIdJavaType,
			MetadataBuildingContext context) {
		final var basicValue = new BasicValue( context, table );
		basicValue.setImplicitJavaTypeAccess( typeConfiguration -> transactionIdJavaType );
		final var column = new Column();
		column.setNullable( nullable );
		column.setValue( basicValue );
		basicValue.addColumn( column );
		final var database = context.getMetadataCollector().getDatabase();
		setTemporalColumnName( columnName, column, database,
				context.getBuildingOptions().getPhysicalNamingStrategy() );
		setTemporalColumnType( temporalPrecision, column, database, transactionIdJavaType );
		return column;
	}

	private static void setTemporalColumnType(
			Integer temporalPrecision,
			Column column,
			Database database,
			Class<?> transactionIdJavaType) {
		if ( temporalPrecision != null ) {
			column.setTemporalPrecision( temporalPrecision );
		}
		else if ( Instant.class.equals( transactionIdJavaType ) ) {
			final var temporalTableSupport = database.getDialect().getTemporalTableSupport();
			column.setTemporalPrecision( temporalTableSupport.getTemporalColumnPrecision() );
			column.setSqlTypeCode( temporalTableSupport.getTemporalColumnType() );
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
			Column rowStartColumn,
			Column rowEndColumn,
			MetadataBuildingContext context) {
		final var dialect = context.getMetadataCollector().getDatabase().getDialect();
		if ( dialect.getTemporalTableSupport()
				.createTemporalTableCheckConstraint( context.getTemporalTableStrategy() ) ) {
			final String rowStartName = rowStartColumn.getQuotedName( dialect );
			final String rowEndName = rowEndColumn.getQuotedName( dialect );
			table.addCheck( new CheckConstraint( rowEndName + " is null or " + rowEndName + " > " + rowStartName ) );
		}
	}

	private static void handleTemporalColumnGeneration(
			Column rowStartColumn, Column rowEndColumn,
			MetadataBuildingContext context) {
		if ( context.getTemporalTableStrategy() == NATIVE ) {
			rowStartColumn.setGeneratedAs( "row start" );
			rowEndColumn.setGeneratedAs( "row end" );
		}
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
			return AUTO;
		}
		else {
			return AUTO;
		}
	}
}
