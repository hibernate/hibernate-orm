/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Audited;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Stateful;
import org.hibernate.mapping.Table;
import org.hibernate.persister.state.internal.AuditStateManagement;
import org.hibernate.temporal.spi.TransactionIdentifierService;

import static org.hibernate.internal.util.StringHelper.isBlank;

/**
 * Helper for building audit log tables in the boot model.
 */
public final class AuditHelper {
	public static final String TRANSACTION_ID = "transactionId";
	public static final String MODIFICATION_TYPE = "modificationType";

	// defaults for backward compatibility with envers

	private static final String DEFAULT_TABLE_SUFFIX = "_aud";

	private AuditHelper() {
	}

	static void bindAuditTable(
			Audited audited,
			RootClass rootClass,
			MetadataBuildingContext context) {
		bindAuditTable( audited, rootClass, context,
				resolveExcludedColumns( rootClass ) );
	}

	static void bindAuditTable(
			Audited audited,
			Collection collection,
			MetadataBuildingContext context) {
		bindAuditTable( audited, collection, context, Set.of() );
	}

	private static void bindAuditTable(
			Audited audited,
			Stateful auditable,
			MetadataBuildingContext context,
			Set<String> excludedColumns) {
		final var collector = context.getMetadataCollector();
		final var table = auditable.getMainTable();
		final String explicitAuditTableName = audited.tableName();
		final boolean hasExplicitAuditTableName = !isBlank( explicitAuditTableName );
		final var auditTable = collector.addTable(
				table.getSchema(),
				table.getCatalog(),
				hasExplicitAuditTableName
						? explicitAuditTableName
						: collector.getLogicalTableName( table )
								+ DEFAULT_TABLE_SUFFIX,
				table.getSubselect(),
				table.isAbstract(),
				context,
				hasExplicitAuditTableName
						|| table.getNameIdentifier().isExplicit()
		);
		collector.addTableNameBinding( table.getNameIdentifier(), auditTable );
		copyTableColumns( table, auditTable, excludedColumns );
		final var transactionIdColumn =
				createAuditColumn( audited.transactionId(),
						getTransactionIdType( context ), auditTable, context );
		final var modificationTypeColumn =
				createAuditColumn( audited.modificationType(),
						Byte.class, auditTable, context );
		auditTable.addColumn( transactionIdColumn );
		auditTable.addColumn( modificationTypeColumn );
		enableAudit( auditable, auditTable, transactionIdColumn, modificationTypeColumn );

		collector.addSecondPass( (OptionalDeterminationSecondPass) ignored ->
				copyTableColumns( table, auditTable, excludedColumns ) );
	}

	static void enableAudit(
			Stateful model, Table auditTable,
			Column transactionIdColumn, Column modificationTypeColumn) {
		model.setAuxiliaryTable( auditTable );
		model.addAuxiliaryColumn( TRANSACTION_ID, transactionIdColumn );
		model.addAuxiliaryColumn( MODIFICATION_TYPE, modificationTypeColumn );
		model.setStateManagementType( AuditStateManagement.class );
	}

	private static Class<?> getTransactionIdType(MetadataBuildingContext context) {
		return context.getBootstrapContext().getServiceRegistry()
				.requireService( TransactionIdentifierService.class )
				.getIdentifierType();
	}

	private static void copyTableColumns(Table sourceTable, Table targetTable, Set<String> excludedColumns) {
		for ( var column : sourceTable.getColumns() ) {
			if ( !excludedColumns.contains( column.getCanonicalName() ) ) {
				targetTable.addColumn( column.clone() );
			}
		}
	}

	private static Column createAuditColumn(
			String columnName,
			Class<?> javaType,
			Table table,
			MetadataBuildingContext context) {
		final var basicValue = new BasicValue( context, table );
		basicValue.setImplicitJavaTypeAccess( typeConfiguration -> javaType );
		final var column = new Column();
		column.setNullable( false );
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
