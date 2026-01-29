/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.annotations.Temporal;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.TemporalTableStrategy;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.CheckConstraint;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Temporalized;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.TemporalMappingImpl;
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.cfg.MappingSettings.TEMPORAL_TABLE_STRATEGY;
import static org.hibernate.cfg.MappingSettings.TRANSACTION_ID_SUPPLIER;
import static org.hibernate.cfg.MappingSettings.USE_SERVER_TRANSACTION_TIMESTAMPS;
import static org.hibernate.cfg.TemporalTableStrategy.AUTO;
import static org.hibernate.cfg.TemporalTableStrategy.HISTORY_TABLE;
import static org.hibernate.cfg.TemporalTableStrategy.NATIVE;
import static org.hibernate.internal.util.GenericsHelper.erasedType;
import static org.hibernate.internal.util.GenericsHelper.supertypeInstantiation;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.internal.util.StringHelper.isBlank;

/**
 * Helper for dealing with {@link org.hibernate.annotations.Temporal}.
 */
public class TemporalHelper {

	public static void bindTemporalColumns(
			Temporal temporal,
			Temporalized target,
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
		final var transactionIdType = resolveTransactionIdJavaType( context );
		final var rowStartColumn =
				createTemporalColumn( temporal.rowStart(),
						table, false, precision, transactionIdType, context );
		final var rowEndColumn =
				createTemporalColumn( temporal.rowEnd(),
						table, true, precision, transactionIdType, context );
		handleTemporalColumnGeneration( rowStartColumn, rowEndColumn, context );

		final var temporalTable =
				usingHistoryTemporalTables( context )
						? createHistoryTable( table, historyTable, context )
						: table;
		temporalTable.addColumn( rowStartColumn );
		temporalTable.addColumn( rowEndColumn );
		target.enableTemporal( rowStartColumn, rowEndColumn, partitioned );
		target.setTemporalTable( temporalTable );

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

	private static String inferredPartitionName(
			String partitionName, String suffix, Table table, InFlightMetadataCollector collector) {
		return partitionName == null || partitionName.isBlank()
				? collector.getLogicalTableName( table ) + '_' + suffix
				: partitionName;
	}

	private static void addSecondPass(Temporalized target, MetadataBuildingContext context) {
		if ( usingHistoryTemporalTables( context )
				&& !suppressesTemporalTablePrimaryKeys( target.isTemporallyPartitioned(), context ) ) {
			context.getMetadataCollector().addSecondPass( (OptionalDeterminationSecondPass) ignored -> {
				copyTableColumns( target.getMainTable(), target.getTemporalTable() );
//				final var primaryKey = target.getTemporalTable().getPrimaryKey();
//				if ( primaryKey == null || primaryKey.getColumnSpan() == 0 ) {
					createHistoryTablePrimaryKey( target, context );
//				}
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
			Temporalized target,
			MetadataBuildingContext context) {
		final var historyTable = target.getTemporalTable();
		final var startingColumn = target.getTemporalStartingColumn();
		if ( target instanceof RootClass rootClass ) {
			final var primaryKey = new PrimaryKey( historyTable );
			addColumnsByName( primaryKey, historyTable,
					rootClass.getKey().getColumns() );
			if ( addPartitionKeyToPrimaryKey( context ) ) {
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
		final var strategy = context.getTemporalTableStrategy( dialect );
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
		final var dialect = database.getDialect();
		dialect.getTemporalTableSupport().addTemporalTableAuxiliaryObjects(
				context.getTemporalTableStrategy( dialect ),
				table, database,
				partitioned,
				currentPartitionName,
				historyPartitionName
		);
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
		final var strategy = context.getTemporalTableStrategy( dialect );
		if ( dialect.getTemporalTableSupport().createTemporalTableCheckConstraint( strategy ) ) {
			final String rowStartName = rowStartColumn.getQuotedName( dialect );
			final String rowEndName = rowEndColumn.getQuotedName( dialect );
			table.addCheck( new CheckConstraint( rowEndName + " is null or " + rowEndName + " > " + rowStartName ) );
		}
	}

	public static boolean usingNativeTemporalTables(MetadataBuildingContext context) {
		final var dialect = context.getMetadataCollector().getDatabase().getDialect();
		return context.getTemporalTableStrategy( dialect ) == NATIVE
			&& dialect.getTemporalTableSupport().supportsNativeTemporalTables();
	}

	public static boolean usingHistoryTemporalTables(MetadataBuildingContext context) {
		final var dialect = context.getMetadataCollector().getDatabase().getDialect();
		return context.getTemporalTableStrategy( dialect ) == HISTORY_TABLE;
	}

	public static boolean suppressesTemporalTablePrimaryKeys(boolean partitioned, MetadataBuildingContext context) {
		return context.getMetadataCollector().getDatabase().getDialect().getTemporalTableSupport()
				.suppressesTemporalTablePrimaryKeys( partitioned );
	}

	private static boolean addPartitionKeyToPrimaryKey(MetadataBuildingContext context) {
		return context.getMetadataCollector().getDatabase().getDialect().addPartitionKeyToPrimaryKey();
	}

	private static void handleTemporalColumnGeneration(
			Column rowStartColumn, Column rowEndColumn,
			MetadataBuildingContext context) {
		if ( usingNativeTemporalTables( context ) ) {
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

	private static Class<?> resolveTransactionIdJavaType(MetadataBuildingContext context) {
		final var serviceRegistry = context.getBootstrapContext().getServiceRegistry();
		final var settings = serviceRegistry.requireService( ConfigurationService.class ).getSettings();
		final boolean useServerTransactionTimestamps = getBoolean( USE_SERVER_TRANSACTION_TIMESTAMPS, settings );
		final Object supplierSetting = settings.get( TRANSACTION_ID_SUPPLIER );
		if ( supplierSetting == null ) {
			return Instant.class;
		}
		else {
			if ( useServerTransactionTimestamps ) {
				throw new MappingException( "Settings '"
							+ USE_SERVER_TRANSACTION_TIMESTAMPS + "' and '"
							+ TRANSACTION_ID_SUPPLIER + "' are mutually exclusive"
				);
			}
			final var supplierClass = resolveSupplierClass( supplierSetting, serviceRegistry );
			final Class<?> suppliedType = resolveSuppliedType( supplierClass );
			if ( suppliedType == null || Object.class.equals( suppliedType ) ) {
				throw new MappingException(
						"Could not determine the Java type of values supplied by '" + supplierClass.getName() + "'"
						+ " (implement 'Supplier<T>' with a concrete type argument)"
				);
			}
			return suppliedType;
		}
	}

	private static Class<? extends Supplier<?>> resolveSupplierClass(
			Object supplierSetting,
			ServiceRegistry serviceRegistry) {
		if ( supplierSetting instanceof Supplier<?> supplier ) {
			@SuppressWarnings("unchecked") // completely safe
			final var supplierClass = (Class<? extends Supplier<?>>) supplier.getClass();
			return supplierClass;
		}
		else if ( supplierSetting instanceof Class<?> supplierClass ) {
			if ( !Supplier.class.isAssignableFrom( supplierClass ) ) {
				throw new MappingException(
						TRANSACTION_ID_SUPPLIER + " must specify a "
						+ Supplier.class.getName() + " or a class name"
				);
			}
			@SuppressWarnings("unchecked") // safe, we just checked
			final var castClass = (Class<? extends Supplier<?>>) supplierClass;
			return castClass;
		}
		else if ( supplierSetting instanceof String supplierName ) {
			final var supplierClass =
					serviceRegistry.requireService( StrategySelector.class )
							.selectStrategyImplementor( Supplier.class, supplierName );
			@SuppressWarnings("unchecked") // safe
			final var castClass = (Class<? extends Supplier<?>>) supplierClass;
			return castClass;
		}
		else {
			throw new MappingException(
					TRANSACTION_ID_SUPPLIER + " must specify a '"
					+ Supplier.class.getName() + "' or a class name"
			);
		}
	}

	private static Class<?> resolveSuppliedType(Class<? extends Supplier<?>> supplierClass) {
		final var supplierInstantiation = supertypeInstantiation( Supplier.class, supplierClass );
		if ( supplierInstantiation == null ) {
			return null;
		}
		else {
			final var typeArguments = supplierInstantiation.getActualTypeArguments();
			return typeArguments.length == 0 ? null : erasedType( typeArguments[0] );
		}
	}

	public static Supplier<?> resolveTransactionIdSupplier(
			Object setting,
			StrategySelector strategySelector) {
		if ( setting == null ) {
			return null;
		}
		else if ( setting instanceof Supplier<?> supplier ) {
			return supplier;
		}
		else if ( setting instanceof Class<?> clazz ) {
			if ( !Supplier.class.isAssignableFrom( clazz ) ) {
				throw new HibernateException(
						"Setting '" + TRANSACTION_ID_SUPPLIER + "' must specify a '"
						+ Supplier.class.getName() + "' or a class name"
				);
			}
			return strategySelector.resolveStrategy( Supplier.class, clazz );
		}
		else if ( setting instanceof String name ) {
			return strategySelector.resolveStrategy( Supplier.class, name );
		}
		else {
			throw new HibernateException(
					"Setting '" + TRANSACTION_ID_SUPPLIER + "' must specify a '"
					+ Supplier.class.getName() + "' or a class name"
			);
		}
	}
}
