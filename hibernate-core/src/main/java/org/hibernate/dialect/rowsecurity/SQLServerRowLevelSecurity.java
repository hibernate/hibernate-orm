/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.boot.Metadata;
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

	private static final String SET_TENANT_SQL =
			"exec sys.sp_set_session_context @key=N'%s', @value=?"
					.formatted( TENANT_IDENTIFIER_CONTEXT_KEY );
	private static final String SET_ROOT_TENANT_SQL =
			"exec sys.sp_set_session_context @key=N'%s', @value=?"
					.formatted( ROOT_TENANT_IDENTIFIER_CONTEXT_KEY );
	private static final String PREDICATE_SQL =
			"@tenant_id = cast(session_context(N'%s') as $TYPE$)"
					.formatted( TENANT_IDENTIFIER_CONTEXT_KEY )
			+ " or cast(session_context(N'%s') as bit) = 1"
					.formatted( ROOT_TENANT_IDENTIFIER_CONTEXT_KEY );

	@Override
	public boolean supportsRowLevelSecurity() {
		return true;
	}

	@Override
	public void addTenantIdTableInitCommands(
			InFlightMetadataCollector collector,
			Table table,
			Column tenantIdentifierColumn,
			Metadata metadata) {
		if ( supportsRowLevelSecurity() ) {
			final String tenantIdentifierColumnType = tenantIdentifierColumn.getSqlType( metadata );
			final var database = collector.getDatabase();
			database.addAuxiliaryDatabaseObject( new PredicateFunction( table, tenantIdentifierColumnType ) );
			database.addAuxiliaryDatabaseObject( new SecurityPolicy( table, tenantIdentifierColumn ) );
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
		final var qualifiedTableName = table.getQualifiedTableName();
		return context.format(
				new QualifiedNameImpl(
						qualifiedTableName.getCatalogName(),
						objectSchema( table, context ),
						qualifiedTableName.getTableName()
				)
		);
	}

	private static Identifier objectSchema(Table table, SqlStringGenerationContext context) {
		final Identifier schema = table.getQualifiedTableName().getSchemaName();
		final Identifier defaultSchema = context.getDefaultSchema();
		if ( schema != null ) {
			return schema;
		}
		else if ( defaultSchema != null ) {
			return defaultSchema;
		}
		else {
			return context.toIdentifier( "dbo" );
		}
	}

	@Override
	public void setTenantIdentifier(Connection connection, String tenantIdentifier, boolean root)
			throws SQLException {
		try ( var statement = connection.prepareStatement( SET_TENANT_SQL ) ) {
			statement.setString( 1, tenantIdentifier );
			statement.execute();
		}
		try ( var statement = connection.prepareStatement( SET_ROOT_TENANT_SQL ) ) {
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

		private String qualifiedPredicateFunctionName(SqlStringGenerationContext context) {
			return qualifiedObjectName( table, predicateFunctionName( table ), context );
		}

		@Override
		public String[] sqlCreateStrings(SqlStringGenerationContext context) {
			return new String[] {
					"create function "
					+ qualifiedPredicateFunctionName( context )
					+ "(@tenant_id " + tenantIdentifierColumnType + ")"
					+ " returns table with schemabinding as"
					+ " return select 1 as hibernate_tenant_isolation_result where "
					+ PREDICATE_SQL.replace( "$TYPE$", tenantIdentifierColumnType )
			};
		}

		@Override
		public String[] sqlDropStrings(SqlStringGenerationContext context) {
			return new String[] { "drop function " + qualifiedPredicateFunctionName( context ) };
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

		private String qualifiedSecurityPolicyName(SqlStringGenerationContext context) {
			return qualifiedObjectName( table, objectBaseName( table ), context );
		}

		@Override
		public String[] sqlCreateStrings(SqlStringGenerationContext context) {
			final String functionName = qualifiedObjectName( table, predicateFunctionName( table ), context );
			final String tableName = qualifiedTableName( table, context );
			final String tenantIdentifierColumnName = tenantIdentifierColumn.getQuotedName( context.getDialect() );
			return new String[] {
					"create security policy "
					+ qualifiedSecurityPolicyName( context )
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
			return new String[] { "drop security policy " + qualifiedSecurityPolicyName( context ) };
		}

		@Override
		public String getExportIdentifier() {
			return "hibernate-row-level-security-sql-server-policy-" + table.getQualifiedTableName();
		}
	}
}
