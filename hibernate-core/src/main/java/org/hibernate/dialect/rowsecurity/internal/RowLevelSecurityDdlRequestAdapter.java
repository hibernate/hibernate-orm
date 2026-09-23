/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity.internal;

import org.hibernate.relational.naming.spi.PhysicalName;

import org.hibernate.relational.naming.spi.QualifiedPhysicalName;


import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdlRequest;
import org.hibernate.dialect.rowsecurity.spi.TenantIdentifierSource;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PhysicalTable;

/**
 * @author Steve Ebersole
 */
final class RowLevelSecurityDdlRequestAdapter implements RowLevelSecurityDdlRequest {
	private final TenantIdentifierSource tenantIdentifierSource;
	private final PhysicalTable table;
	private final Column tenantColumn;
	private final Metadata metadata;
	private final SqlStringGenerationContext context;

	RowLevelSecurityDdlRequestAdapter(
			TenantIdentifierSource tenantIdentifierSource,
			PhysicalTable table,
			Column tenantColumn,
			Metadata metadata,
			SqlStringGenerationContext context) {
		this.tenantIdentifierSource = tenantIdentifierSource;
		this.table = table;
		this.tenantColumn = tenantColumn;
		this.metadata = metadata;
		this.context = context;
	}

	@Override
	public TenantIdentifierSource tenantIdentifierSource() {
		return tenantIdentifierSource;
	}

	@Override
	public String qualifiedTableName() {
		return table.getQualifiedName( context );
	}

	@Override
	public String qualifiedTableName(String defaultSchema) {
		final var name = table.getPhysicalName();
		return context.format( new QualifiedPhysicalName(
				name.getCatalogName(),
				schema( name.getSchemaName(), defaultSchema ),
				name.objectName()
		) );
	}

	@Override
	public String qualifySiblingObject(String objectName, String defaultSchema) {
		if ( objectName == null || objectName.isBlank() ) {
			throw new IllegalArgumentException( "Sibling object name must not be blank" );
		}
		return context.format( new QualifiedPhysicalName(
				null,
				schema( table.getPhysicalName().getSchemaName(), defaultSchema ),
				physical( context.toIdentifier( objectName ) )
		) );
	}

	private PhysicalName schema(PhysicalName mappedSchema, String fallbackSchema) {
		if ( mappedSchema != null ) {
			return mappedSchema;
		}
		final Identifier configuredSchema = context.getDefaultSchema();
		return physical( configuredSchema != null ? configuredSchema : context.toIdentifier( fallbackSchema ) );
	}

	private PhysicalName physical(Identifier identifier) {
		return identifier == null ? null : context.getPhysicalNameFactory().create( identifier.getText(), identifier.isQuoted() );
	}

	@Override
	public String tableExportIdentifier() {
		return table.getExportIdentifier();
	}

	@Override
	public String tenantColumnName() {
		return tenantColumn.getQuotedName( context.getDialect() );
	}

	@Override
	public String tenantColumnSqlType() {
		return tenantColumn.getSqlType( metadata );
	}

	@Override
	public int tenantColumnSqlTypeCode() {
		return tenantColumn.getSqlTypeCode( metadata );
	}
}
