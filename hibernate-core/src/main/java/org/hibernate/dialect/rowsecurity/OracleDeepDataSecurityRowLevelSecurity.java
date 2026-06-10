/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.internal.util.config.ConfigurationException;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_DEEP_DATA_SECURITY_ROOT_TENANT_IDENTIFIER_ATTRIBUTE;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_DEEP_DATA_SECURITY_TENANT_DATABASE_ROLE;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_DEEP_DATA_SECURITY_TENANT_CONTEXT_NAME;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_DEEP_DATA_SECURITY_TENANT_DATA_GRANTEE;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_DEEP_DATA_SECURITY_TENANT_IDENTIFIER_ATTRIBUTE;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_DEEP_DATA_SECURITY_USE_DATA_GRANTS_ONLY;
import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.internal.util.config.ConfigurationHelper.getString;

/**
 * Row-level security support for Oracle Deep Data Security.
 *
 * @author Gavin King
 */
public class OracleDeepDataSecurityRowLevelSecurity implements RowLevelSecurity {
	public static final String DEFAULT_TENANT_IDENTIFIER_ATTRIBUTE = "tenant_id";
	public static final String DEFAULT_ROOT_TENANT_IDENTIFIER_ATTRIBUTE = "tenant_id_root";
	public static final String TENANT_ISOLATION_DATA_GRANT = "hibernate_tenant_isolation";
	public static final String CONTEXT_ACCESS_DATA_GRANT = "hibernate_tenant_context";
	public static final String CONTEXT_PACKAGE = "hibernate_tenant_context";

	private final String tenantContextName;
	private final String tenantContextOwner;
	private final String tenantContextSimpleName;
	private final String tenantIdentifierAttributeName;
	private final String rootTenantIdentifierAttributeName;
	private final String tenantDataGrantee;
	private final String tenantDatabaseRole;
	private final String contextPackageName;
	private final boolean useDataGrantsOnly;

	public static RowLevelSecurity fromSettings(Map<?, ?> settings) {
		final String tenantContextName =
				getString( ORACLE_DEEP_DATA_SECURITY_TENANT_CONTEXT_NAME,
						settings, "DEVELOPER.HIBERNATE_TENANCY" );
		final String tenantDataGrantee =
				getString( ORACLE_DEEP_DATA_SECURITY_TENANT_DATA_GRANTEE,
						settings, "hibernate_dds_role" );
		final String tenantDatabaseRole =
				getString( ORACLE_DEEP_DATA_SECURITY_TENANT_DATABASE_ROLE,
						settings, "hibernate_dds_database_role" );
//		if ( tenantContextName == null && tenantDataGrantee == null && tenantDatabaseRole == null ) {
//			return NoRowLevelSecurity.INSTANCE;
//		}
//		if ( tenantContextName == null || tenantDataGrantee == null || tenantDatabaseRole == null ) {
//			throw new ConfigurationException(
//					"Oracle Deep Data Security row-level security requires '"
//							+ ORACLE_DEEP_DATA_SECURITY_TENANT_CONTEXT_NAME
//							+ "', '"
//							+ ORACLE_DEEP_DATA_SECURITY_TENANT_DATA_GRANTEE
//							+ "', and '"
//							+ ORACLE_DEEP_DATA_SECURITY_TENANT_DATABASE_ROLE
//							+ "'"
//			);
//		}
		return new OracleDeepDataSecurityRowLevelSecurity(
				tenantContextName,
				getString(
						ORACLE_DEEP_DATA_SECURITY_TENANT_IDENTIFIER_ATTRIBUTE,
						settings,
						DEFAULT_TENANT_IDENTIFIER_ATTRIBUTE
				),
				getString(
						ORACLE_DEEP_DATA_SECURITY_ROOT_TENANT_IDENTIFIER_ATTRIBUTE,
						settings,
						DEFAULT_ROOT_TENANT_IDENTIFIER_ATTRIBUTE
				),
				tenantDataGrantee,
				tenantDatabaseRole,
				getBoolean( ORACLE_DEEP_DATA_SECURITY_USE_DATA_GRANTS_ONLY, settings, true )
		);
	}

	public OracleDeepDataSecurityRowLevelSecurity(
			String tenantContextName,
			String tenantIdentifierAttributeName,
			String rootTenantIdentifierAttributeName,
			String tenantDataGrantee,
			String tenantDatabaseRole,
			boolean useDataGrantsOnly) {
		final var contextName = parseContextName( tenantContextName );
		this.tenantContextName = tenantContextName;
		this.tenantContextOwner = contextName.owner();
		this.tenantContextSimpleName = contextName.name();
		this.tenantIdentifierAttributeName = tenantIdentifierAttributeName;
		this.rootTenantIdentifierAttributeName = rootTenantIdentifierAttributeName;
		this.tenantDataGrantee = tenantDataGrantee;
		this.tenantDatabaseRole = tenantDatabaseRole;
		this.contextPackageName = tenantContextOwner + "." + contextPackageName( tenantContextName );
		this.useDataGrantsOnly = useDataGrantsOnly;
	}

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
		collector.getDatabase().addAuxiliaryDatabaseObject(
				new EndUserContext(
						tenantContextName,
						tenantContextOwner,
						tenantContextSimpleName,
						tenantIdentifierAttributeName,
						rootTenantIdentifierAttributeName,
						tenantDataGrantee,
						tenantDatabaseRole,
						contextPackageName
				)
		);
		RowLevelSecurity.super.addTenantIdTableInitCommands(
				collector,
				table,
				tenantIdentifierColumn,
				tenantIdentifierColumnType
		);
	}

	@Override
	public String[] getTenantIdTableCreateStrings(
			Table table,
			Column tenantIdentifierColumn,
			String tenantIdentifierColumnType,
			SqlStringGenerationContext context) {
		final String tableName = table.getQualifiedName( context );
		final String grantName = dataGrantName( table );
		final String dataGrant = "create or replace data grant " + grantName
				+ " as select, insert, update, delete on " + tableName
				+ " where " + tenantDiscriminatorPredicate( tenantIdentifierColumn, context )
				+ " to " + tenantDataGrantee;
		if ( useDataGrantsOnly ) {
			return new String[] {
					dataGrant,
					"set use data grants only on " + tableName + " enabled"
			};
		}
		return new String[] { dataGrant };
	}

	@Override
	public String getTenantIdentifierSettingName() {
		return tenantContextName + "." + tenantIdentifierAttributeName;
	}

	@Override
	public String getRootTenantIdentifierSettingName() {
		return tenantContextName + "." + rootTenantIdentifierAttributeName;
	}

	private String tenantDiscriminatorPredicate(
			Column tenantIdentifierColumn,
			SqlStringGenerationContext context) {
		return tenantIdentifierColumn.getQuotedName( context.getDialect() )
				+ " = " + endUserContextAttribute( tenantIdentifierAttributeName )
				+ " or " + endUserContextAttribute( rootTenantIdentifierAttributeName ) + " = 'true'";
	}

	private String endUserContextAttribute(String attributeName) {
		return "ORA_END_USER_CONTEXT." + tenantContextName + "." + attributeName;
	}

	@Override
	public void setTenantIdentifier(Connection connection, String tenantIdentifier, boolean root) throws SQLException {
		try ( var statement = connection.prepareCall( "begin " + contextPackageName + ".set_tenant(?, ?); end;" ) ) {
			statement.setString( 1, tenantIdentifier );
			statement.setString( 2, Boolean.toString( root ) );
			statement.execute();
		}
	}

	@Override
	public void clearTenantIdentifier(Connection connection, ServiceRegistry serviceRegistry) throws SQLException {
		setTenantIdentifier( connection, "", false );
	}

	private static String dataGrantName(Table table) {
		return TENANT_ISOLATION_DATA_GRANT + "_" + Integer.toUnsignedString(
				table.getQualifiedTableName().toString().hashCode(),
				36
		);
	}

	private static String contextDataGrantName(String tenantContextName) {
		return CONTEXT_ACCESS_DATA_GRANT + "_" + hash( tenantContextName );
	}

	private static String contextPackageName(String tenantContextName) {
		return CONTEXT_PACKAGE + "_" + hash( tenantContextName );
	}

	private static String hash(String value) {
		return Integer.toUnsignedString( value.hashCode(), 36 );
	}

	private static ContextName parseContextName(String tenantContextName) {
		final int separator = tenantContextName.indexOf( '.' );
		if ( separator < 1 || separator == tenantContextName.length() - 1 || tenantContextName.indexOf( '.', separator + 1 ) > 0 ) {
			throw new ConfigurationException(
					"Oracle Deep Data Security tenant context name must be qualified as OWNER.NAME: "
							+ tenantContextName
			);
		}
		return new ContextName( tenantContextName.substring( 0, separator ), tenantContextName.substring( separator + 1 ) );
	}

	private static String trimToNull(String value) {
		return isBlank( value ) ? null : value.trim();
	}

	private static String sqlLiteral(String value) {
		return "'" + value.replace( "'", "''" ) + "'";
	}

	private static String jsonString(String value) {
		return value.replace( "\\", "\\\\" ).replace( "\"", "\\\"" );
	}

	private record ContextName(String owner, String name) {
	}

	private record EndUserContext(
			String tenantContextName,
			String tenantContextOwner,
			String tenantContextSimpleName,
			String tenantIdentifierAttributeName,
			String rootTenantIdentifierAttributeName,
			String tenantDataGrantee,
			String tenantDatabaseRole,
			String contextPackageName) implements AuxiliaryDatabaseObject {

		@Override
		public boolean appliesToDialect(Dialect dialect) {
			return dialect instanceof OracleDialect;
		}

		@Override
		public boolean beforeTablesOnCreation() {
			return true;
		}

		@Override
		public String[] sqlCreateStrings(SqlStringGenerationContext context) {
			final String contextDataGrantName = tenantContextOwner + "."
					+ contextDataGrantName( tenantContextName );
			return new String[] {
					"create or replace end user context " + tenantContextName
							+ " using json schema '" + jsonSchema() + "'",
					"create or replace package " + contextPackageName
							+ " authid current_user as"
							+ " procedure set_tenant(p_tenant_id varchar2, p_tenant_id_root varchar2); end;",
					"create or replace package body " + contextPackageName
							+ " as procedure set_tenant(p_tenant_id varchar2, p_tenant_id_root varchar2) is"
							+ " begin execute immediate "
							+ sqlLiteral(
									"update sys.end_user_context t"
											+ " set t.context." + tenantIdentifierAttributeName + " = :tenant_id,"
											+ " t.context." + rootTenantIdentifierAttributeName + " = :tenant_id_root"
											+ " where owner = '" + tenantContextOwner + "'"
											+ " and name = '" + tenantContextSimpleName + "'"
							)
							+ " using p_tenant_id, p_tenant_id_root; end; end;",
					"grant execute on " + contextPackageName + " to " + tenantDatabaseRole,
					"create or replace data grant " + contextDataGrantName
							+ " as select, update on sys.end_user_context"
							+ " where owner = " + sqlLiteral( tenantContextOwner )
							+ " and name = " + sqlLiteral( tenantContextSimpleName )
							+ " to " + tenantDataGrantee
			};
		}

		private String jsonSchema() {
			return "{\"type\":\"object\",\"properties\":{"
					+ "\"" + jsonString( tenantIdentifierAttributeName ) + "\":{\"type\":[\"string\",\"null\"],\"default\":null},"
					+ "\"" + jsonString( rootTenantIdentifierAttributeName ) + "\":{\"type\":\"string\",\"default\":\"false\"}"
					+ "}}";
		}

		@Override
		public String[] sqlDropStrings(SqlStringGenerationContext context) {
			return new String[] {
					"drop data grant if exists " + tenantContextOwner + "." + contextDataGrantName( tenantContextName ),
					"drop package if exists " + contextPackageName,
					"drop end user context if exists " + tenantContextName
			};
		}

		@Override
		public String getExportIdentifier() {
			return "hibernate-row-level-security-oracle-context-" + tenantContextName;
		}
	}
}
