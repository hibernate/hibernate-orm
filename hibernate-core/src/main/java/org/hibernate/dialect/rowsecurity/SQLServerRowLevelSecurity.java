/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

/**
 * Row-level security support for SQL Server.
 *
 * @author Gavin King
 */
public class SQLServerRowLevelSecurity implements RowLevelSecurity {
	public static final SQLServerRowLevelSecurity INSTANCE = new SQLServerRowLevelSecurity();

	public static final String TENANT_IDENTIFIER_CONTEXT_KEY = "hibernate.tenant_id";
	public static final String ROOT_TENANT_IDENTIFIER_CONTEXT_KEY = "hibernate.tenant_id_root";
	public static final String TENANT_ISOLATION_POLICY = "hibernate_tenant_isolation";

	@Override
	public boolean supportsRowLevelSecurity() {
		return true;
	}

	@Override
	public void addTenantIdTableInitCommands(
			InFlightMetadataCollector collector,
			Table table,
			Column tenantIdentifierColumn,
			String tenantIdentifierColumnType) {
		if ( supportsRowLevelSecurity() ) {
			collector.getDatabase().addAuxiliaryDatabaseObject(
					new PredicateFunction( table, tenantIdentifierColumnType )
			);
			collector.getDatabase().addAuxiliaryDatabaseObject(
					new SecurityPolicy( table, tenantIdentifierColumn )
			);
		}
	}

	private static String objectBaseName(Table table) {
		return TENANT_ISOLATION_POLICY + "_" + Integer.toUnsignedString(
				table.getQualifiedTableName().toString().hashCode(),
				36
		);
	}

	private static String predicateFunctionName(Table table) {
		return objectBaseName( table ) + "_predicate";
	}

	private static String qualifiedObjectName(Table table, String name, SqlStringGenerationContext context) {
		return context.format(
				new QualifiedNameImpl(
						null,
						objectSchema( table, context ),
						context.toIdentifier( name )
				)
		);
	}

	private static String qualifiedTableName(Table table, SqlStringGenerationContext context) {
		return context.format(
				new QualifiedNameImpl(
						table.getQualifiedTableName().getCatalogName(),
						objectSchema( table, context ),
						table.getQualifiedTableName().getTableName()
				)
		);
	}

	private static Identifier objectSchema(Table table, SqlStringGenerationContext context) {
		final Identifier schema = table.getQualifiedTableName().getSchemaName();
		return schema != null ? schema
				: context.getDefaultSchema() != null ? context.getDefaultSchema()
				: context.toIdentifier( "dbo" );
	}

	private static String tenantPredicate(String tenantIdentifierColumnType) {
		return "@tenant_id = cast(session_context(N'" + TENANT_IDENTIFIER_CONTEXT_KEY + "') as "
				+ tenantIdentifierColumnType + ")"
				+ " or cast(session_context(N'" + ROOT_TENANT_IDENTIFIER_CONTEXT_KEY + "') as bit) = 1";
	}

	@Override
	public void setTenantIdentifier(Connection connection, String tenantIdentifier, boolean root) throws SQLException {
		try ( var statement = connection.prepareStatement(
				"exec sys.sp_set_session_context @key=N'" + TENANT_IDENTIFIER_CONTEXT_KEY + "', @value=?"
		) ) {
			statement.setString( 1, tenantIdentifier );
			statement.execute();
		}
		try ( var statement = connection.prepareStatement(
				"exec sys.sp_set_session_context @key=N'" + ROOT_TENANT_IDENTIFIER_CONTEXT_KEY + "', @value=?"
		) ) {
			statement.setInt( 1, root ? 1 : 0 );
			statement.execute();
		}
	}

	@Override
	public String getTenantIdentifierSettingName() {
		return TENANT_IDENTIFIER_CONTEXT_KEY;
	}

	@Override
	public String getRootTenantIdentifierSettingName() {
		return ROOT_TENANT_IDENTIFIER_CONTEXT_KEY;
	}

	private record PredicateFunction(Table table, String tenantIdentifierColumnType)
			implements AuxiliaryDatabaseObject {

		@Override
		public boolean appliesToDialect(Dialect dialect) {
			return dialect instanceof SQLServerDialect;
		}

		@Override
		public boolean beforeTablesOnCreation() {
			return true;
		}

		@Override
		public String[] sqlCreateStrings(SqlStringGenerationContext context) {
			return new String[] {
					"create function " + qualifiedObjectName( table, predicateFunctionName( table ), context )
					+ "(@tenant_id " + tenantIdentifierColumnType + ")"
					+ " returns table"
					+ " with schemabinding"
					+ " as"
					+ " return select 1 as hibernate_tenant_isolation_result"
					+ " where " + tenantPredicate( tenantIdentifierColumnType )
			};
		}

		@Override
		public String[] sqlDropStrings(SqlStringGenerationContext context) {
			return new String[] {
					"drop function " + qualifiedObjectName( table, predicateFunctionName( table ), context )
			};
		}

		@Override
		public String getExportIdentifier() {
			return "hibernate-row-level-security-sql-server-function-" + table.getQualifiedTableName();
		}
	}

	private record SecurityPolicy(Table table, Column tenantIdentifierColumn)
			implements AuxiliaryDatabaseObject {

		@Override
		public boolean appliesToDialect(Dialect dialect) {
			return dialect instanceof SQLServerDialect;
		}

		@Override
		public boolean beforeTablesOnCreation() {
			return false;
		}

		@Override
		public String[] sqlCreateStrings(SqlStringGenerationContext context) {
			final String policyName = qualifiedObjectName( table, objectBaseName( table ), context );
			final String functionName = qualifiedObjectName( table, predicateFunctionName( table ), context );
			final String tableName = qualifiedTableName( table, context );
			final String tenantIdentifierColumnName = tenantIdentifierColumn.getQuotedName( context.getDialect() );
			return new String[] {
					"create security policy " + policyName
					+ " add filter predicate " + functionName + "(" + tenantIdentifierColumnName + ")"
					+ " on " + tableName + ","
					+ " add block predicate " + functionName + "(" + tenantIdentifierColumnName + ")"
					+ " on " + tableName + " after insert,"
					+ " add block predicate " + functionName + "(" + tenantIdentifierColumnName + ")"
					+ " on " + tableName + " after update"
					+ " with (state = on)"
			};
		}

		@Override
		public String[] sqlDropStrings(SqlStringGenerationContext context) {
			return new String[] {
					"drop security policy " + qualifiedObjectName( table, objectBaseName( table ), context )
			};
		}

		@Override
		public String getExportIdentifier() {
			return "hibernate-row-level-security-sql-server-policy-" + table.getQualifiedTableName();
		}
	}
}
