/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity.internal;

import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.AbstractAuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurity;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdl;
import org.hibernate.dialect.rowsecurity.spi.TenantIdentifierSource;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

/**
 * Adapts declarative row-level-security DDL to the internal schema model.
 *
 * @author Steve Ebersole
 */
public final class RowLevelSecurityDdlMaterializer {
	private RowLevelSecurityDdlMaterializer() {
	}

	public static void materialize(
			RowLevelSecurity strategy,
			TenantIdentifierSource tenantIdentifierSource,
			Table table,
			Column tenantColumn,
			Metadata metadata,
			Map<String, Object> configurationValues) {
		final Database database = metadata.getDatabase();
		final SqlStringGenerationContext registrationContext =
				SqlStringGenerationContextImpl.fromConfigurationMap(
						database.getJdbcEnvironment(),
						database,
						configurationValues
				);
		final var request = new RowLevelSecurityDdlRequestAdapter(
				tenantIdentifierSource,
				table,
				tenantColumn,
				metadata,
				registrationContext
		);
		for ( var descriptor : strategy.getTenantTableDdl( request ) ) {
			database.addAuxiliaryDatabaseObject( new MaterializedDdl(
					strategy,
					tenantIdentifierSource,
					table,
					tenantColumn,
					metadata,
					descriptor
			) );
		}
	}

	private static final class MaterializedDdl extends AbstractAuxiliaryDatabaseObject {
		private final RowLevelSecurity strategy;
		private final TenantIdentifierSource tenantIdentifierSource;
		private final Table table;
		private final Column tenantColumn;
		private final Metadata metadata;
		private final String exportIdentifier;
		private final RowLevelSecurityDdl.Phase phase;

		private MaterializedDdl(
				RowLevelSecurity strategy,
				TenantIdentifierSource tenantIdentifierSource,
				Table table,
				Column tenantColumn,
				Metadata metadata,
				RowLevelSecurityDdl descriptor) {
			super(
					descriptor.phase() == RowLevelSecurityDdl.Phase.BEFORE_TABLES,
					descriptor.dialectScopes()
			);
			this.strategy = strategy;
			this.tenantIdentifierSource = tenantIdentifierSource;
			this.table = table;
			this.tenantColumn = tenantColumn;
			this.metadata = metadata;
			this.exportIdentifier = descriptor.exportIdentifier();
			this.phase = descriptor.phase();
		}

		@Override
		public String getExportIdentifier() {
			return exportIdentifier;
		}

		@Override
		public String[] sqlCreateStrings(SqlStringGenerationContext context) {
			return descriptor( context ).createCommands().toArray( String[]::new );
		}

		@Override
		public String[] sqlDropStrings(SqlStringGenerationContext context) {
			return descriptor( context ).dropCommands().toArray( String[]::new );
		}

		private RowLevelSecurityDdl descriptor(SqlStringGenerationContext context) {
			final var request = new RowLevelSecurityDdlRequestAdapter(
					tenantIdentifierSource,
					table,
					tenantColumn,
					metadata,
					context
			);
			for ( var descriptor : strategy.getTenantTableDdl( request ) ) {
				if ( descriptor.phase() == phase
						&& descriptor.exportIdentifier().equals( exportIdentifier ) ) {
					return descriptor;
				}
			}
			throw new IllegalStateException(
					"Row-level-security strategy no longer returned DDL descriptor '" + exportIdentifier + "'"
			);
		}
	}
}
