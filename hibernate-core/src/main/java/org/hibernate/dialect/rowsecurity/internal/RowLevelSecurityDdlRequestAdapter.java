/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity.internal;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdlRequest;
import org.hibernate.dialect.rowsecurity.spi.TenantIdentifierSource;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

/**
 * @author Steve Ebersole
 */
final class RowLevelSecurityDdlRequestAdapter implements RowLevelSecurityDdlRequest {
	private final TenantIdentifierSource tenantIdentifierSource;
	private final Table table;
	private final Column tenantColumn;
	private final Metadata metadata;
	private final SqlStringGenerationContext context;

	RowLevelSecurityDdlRequestAdapter(
			TenantIdentifierSource tenantIdentifierSource,
			Table table,
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
		final var name = table.getQualifiedTableName();
		return context.format( new QualifiedTableName(
				name.getCatalogName(),
				schema( name.getSchemaName(), defaultSchema ),
				name.getTableName()
		) );
	}

	@Override
	public String qualifySiblingObject(String objectName, String defaultSchema) {
		if ( objectName == null || objectName.isBlank() ) {
			throw new IllegalArgumentException( "Sibling object name must not be blank" );
		}
		return context.format( new QualifiedNameImpl(
				null,
				schema( table.getQualifiedTableName().getSchemaName(), defaultSchema ),
				context.toIdentifier( objectName )
		) );
	}

	private Identifier schema(Identifier mappedSchema, String fallbackSchema) {
		if ( mappedSchema != null ) {
			return mappedSchema;
		}
		final Identifier configuredSchema = context.getDefaultSchema();
		return configuredSchema != null ? configuredSchema : context.toIdentifier( fallbackSchema );
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
