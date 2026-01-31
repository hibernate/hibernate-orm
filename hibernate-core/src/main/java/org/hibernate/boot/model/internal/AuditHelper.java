/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.annotations.Audited;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.mapping.Auditable;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.internal.AuditMappingImpl;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.internal.util.GenericsHelper.erasedType;
import static org.hibernate.internal.util.GenericsHelper.supertypeInstantiation;
import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;

/**
 * Helper for building audit log tables in the boot model.
 */
public final class AuditHelper {
	private static final String DEFAULT_TABLE_SUFFIX = "_aud";
	private static final String DEFAULT_TRANSACTION_ID_COLUMN = "REV";
	private static final String DEFAULT_MODIFICATION_TYPE_COLUMN = "REVTYPE";

	private AuditHelper() {
	}

	static void bindAuditTable(
			Audited audited,
			RootClass rootClass,
			MetadataBuildingContext context) {
		if ( audited != null ) {
			final var table = rootClass.getRootTable();
			if ( table != null ) {
				final var collector = context.getMetadataCollector();
				final String auditTableName = resolveTableName( audited, table, collector );
				final boolean explicitName = isExplicitName( audited, table );
				final Table auditTable = collector.addTable(
						table.getSchema(),
						table.getCatalog(),
						auditTableName,
						table.getSubselect(),
						table.isAbstract(),
						context,
						explicitName
				);
				collector.addTableNameBinding( table.getNameIdentifier(), auditTable );

				final var excludedColumns = resolveExcludedColumns( rootClass );
				copyTableColumns( table, auditTable, excludedColumns );
				final Column transactionIdColumn = createTransactionIdColumn( audited, auditTable, context );
				final Column modificationTypeColumn = createModificationTypeColumn( audited, auditTable, context );
				auditTable.addColumn( transactionIdColumn );
				auditTable.addColumn( modificationTypeColumn );
				rootClass.enableAudit( auditTable, transactionIdColumn, modificationTypeColumn );

				collector.addSecondPass( (OptionalDeterminationSecondPass) ignored -> copyTableColumns( table, auditTable, excludedColumns ) );
			}
		}
	}

	static void bindAuditTable(
			Audited audited,
			Collection collection,
			MetadataBuildingContext context) {
		if ( audited != null ) {
			final var table = collection.getCollectionTable();
			if ( table != null ) {
				final var collector = context.getMetadataCollector();
				final String auditTableName = resolveTableName( audited, table, collector );
				final boolean explicitName = isExplicitName( audited, table );
				final var auditTable = collector.addTable(
						table.getSchema(),
						table.getCatalog(),
						auditTableName,
						table.getSubselect(),
						table.isAbstract(),
						context,
						explicitName
				);
				collector.addTableNameBinding( table.getNameIdentifier(), auditTable );

				copyTableColumns( table, auditTable, Set.of() );
				final var transactionIdColumn = createTransactionIdColumn( audited, auditTable, context );
				final var modificationTypeColumn = createModificationTypeColumn( audited, auditTable, context );
				auditTable.addColumn( transactionIdColumn );
				auditTable.addColumn( modificationTypeColumn );
				collection.enableAudit( auditTable, transactionIdColumn, modificationTypeColumn );

				collector.addSecondPass( (OptionalDeterminationSecondPass) ignored -> copyTableColumns( table, auditTable, Set.of() ) );
			}
		}
	}

	private static String resolveTableName(
			Audited audited,
			Table table,
			InFlightMetadataCollector collector) {
		final String explicitName = audited == null ? null : audited.tableName();
		if ( isBlank( explicitName ) ) {
			final String baseName = table.getName() != null
					? table.getName()
					: collector.getLogicalTableName( table );
			return baseName + DEFAULT_TABLE_SUFFIX;
		}
		return explicitName;
	}

	private static boolean isExplicitName(Audited audited, Table table) {
		final String explicitName = audited == null ? null : audited.tableName();
		return !isBlank( explicitName )
			|| table.getName() != null
			|| table.getNameIdentifier().isExplicit();
	}

	private static void copyTableColumns(Table sourceTable, Table targetTable, Set<String> excludedColumns) {
		for ( var column : sourceTable.getColumns() ) {
			if ( !excludedColumns.contains( column.getCanonicalName() ) ) {
				targetTable.addColumn( column.clone() );
			}
		}
	}

	private static Column createTransactionIdColumn(
			Audited audited,
			Table table,
			MetadataBuildingContext context) {
		final String columnName = resolveTransactionIdColumn( audited );
		final var transactionIdJavaType = resolveTransactionIdJavaType( context );
		return createAuditColumn( columnName, table, false, transactionIdJavaType, context );
	}

	private static Column createModificationTypeColumn(
			Audited audited,
			Table table,
			MetadataBuildingContext context) {
		final String columnName = resolveModificationTypeColumn( audited );
		return createAuditColumn( columnName, table, false, Integer.class, context );
	}

	private static Column createAuditColumn(
			String columnName,
			Table table,
			boolean nullable,
			Class<?> javaType,
			MetadataBuildingContext context) {
		final var basicValue = new BasicValue( context, table );
		basicValue.setImplicitJavaTypeAccess( typeConfiguration -> javaType );
		final var column = new Column();
		column.setNullable( nullable );
		column.setValue( basicValue );
		basicValue.addColumn( column );

		final var database = context.getMetadataCollector().getDatabase();
		setColumnName( columnName, column, database, context.getBuildingOptions().getPhysicalNamingStrategy() );
		setTemporalColumnType( column, database, javaType );

		return column;
	}

	private static void setTemporalColumnType(
			Column column,
			Database database,
			Class<?> javaType) {
		if ( Instant.class.equals( javaType ) ) {
			final var temporalTableSupport = database.getDialect().getTemporalTableSupport();
			column.setTemporalPrecision( temporalTableSupport.getTemporalColumnPrecision() );
			column.setSqlTypeCode( temporalTableSupport.getTemporalColumnType() );
		}
	}

	private static void setColumnName(
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

	public static AuditMappingImpl resolveAuditMapping(
			Auditable bootMapping,
			String tableName,
			MappingModelCreationProcess creationProcess) {
		return bootMapping.isAudited()
				? new AuditMappingImpl( bootMapping, tableName, creationProcess )
				: null;
	}

	private static String resolveTransactionIdColumn(Audited audited) {
		final String explicitName = audited == null ? null : audited.transactionId();
		return isBlank( explicitName ) ? DEFAULT_TRANSACTION_ID_COLUMN : explicitName;
	}

	private static String resolveModificationTypeColumn(Audited audited) {
		final String explicitName = audited == null ? null : audited.modificationType();
		return isBlank( explicitName ) ? DEFAULT_MODIFICATION_TYPE_COLUMN : explicitName;
	}

	private static Class<?> resolveTransactionIdJavaType(MetadataBuildingContext context) {
		final var serviceRegistry = context.getBootstrapContext().getServiceRegistry();
		final var settings = serviceRegistry.requireService( ConfigurationService.class ).getSettings();
		final boolean useServerTransactionTimestamps =
				getBoolean( MappingSettings.USE_SERVER_TRANSACTION_TIMESTAMPS, settings );
		final Object supplierSetting = settings.get( MappingSettings.TRANSACTION_ID_SUPPLIER );
		if ( supplierSetting == null ) {
			return Instant.class;
		}
		if ( useServerTransactionTimestamps ) {
			throw new MappingException( "Settings '"
					+ MappingSettings.USE_SERVER_TRANSACTION_TIMESTAMPS + "' and '"
					+ MappingSettings.TRANSACTION_ID_SUPPLIER + "' are mutually exclusive" );
		}

		final Class<? extends Supplier<?>> supplierClass = resolveSupplierClass( supplierSetting, serviceRegistry );
		final Class<?> suppliedType = resolveSuppliedType( supplierClass );
		if ( suppliedType == null || Object.class.equals( suppliedType ) ) {
			throw new MappingException( "Could not determine the Java type of values supplied by '"
					+ supplierClass.getName() + "'"
					+ " (implement 'Supplier<T>' with a concrete type argument)" );
		}
		return suppliedType;
	}

	private static Class<? extends Supplier<?>> resolveSupplierClass(
			Object supplierSetting,
			ServiceRegistry serviceRegistry) {
		if ( supplierSetting instanceof Supplier<?> supplier ) {
			@SuppressWarnings("unchecked")
			final var supplierClass = (Class<? extends Supplier<?>>) supplier.getClass();
			return supplierClass;
		}
		else if ( supplierSetting instanceof Class<?> supplierClass ) {
			if ( !Supplier.class.isAssignableFrom( supplierClass ) ) {
				throw new MappingException( MappingSettings.TRANSACTION_ID_SUPPLIER + " must specify a "
						+ Supplier.class.getName() + " or a class name" );
			}
			@SuppressWarnings("unchecked")
			final var castClass = (Class<? extends Supplier<?>>) supplierClass;
			return castClass;
		}
		else if ( supplierSetting instanceof String supplierName ) {
			final var supplierClass =
					serviceRegistry.requireService( StrategySelector.class )
							.selectStrategyImplementor( Supplier.class, supplierName );
			@SuppressWarnings("unchecked")
			final var castClass = (Class<? extends Supplier<?>>) supplierClass;
			return castClass;
		}
		else {
			throw new MappingException( MappingSettings.TRANSACTION_ID_SUPPLIER + " must specify a '"
						+ Supplier.class.getName() + "' or a class name" );
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

	private static Set<String> resolveExcludedColumns(RootClass rootClass) {
		final Set<String> excluded = new HashSet<>();
		final var properties = rootClass.getProperties();
		for ( var property : properties ) {
			if ( property.isAuditedExcluded() ) {
				for ( var column : property.getColumns() ) {
					excluded.add( column.getCanonicalName() );
				}
			}
		}
		return excluded;
	}
}
