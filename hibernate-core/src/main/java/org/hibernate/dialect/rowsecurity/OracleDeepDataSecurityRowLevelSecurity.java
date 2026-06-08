/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.util.config.ConfigurationException;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_DEEP_DATA_SECURITY_CONTEXT_PROVIDER;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_DEEP_DATA_SECURITY_ROOT_TENANT_IDENTIFIER_ATTRIBUTE;
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

	private final String tenantContextName;
	private final String tenantIdentifierAttributeName;
	private final String rootTenantIdentifierAttributeName;
	private final String tenantDataGrantee;
	private final boolean useDataGrantsOnly;

	public static RowLevelSecurity fromSettings(Map<?, ?> settings) {
		final String tenantContextName = trimToNull(
				getString( ORACLE_DEEP_DATA_SECURITY_TENANT_CONTEXT_NAME, settings )
		);
		final String tenantDataGrantee = trimToNull(
				getString( ORACLE_DEEP_DATA_SECURITY_TENANT_DATA_GRANTEE, settings )
		);
		if ( tenantContextName == null && tenantDataGrantee == null ) {
			return NoRowLevelSecurity.INSTANCE;
		}
		if ( tenantContextName == null || tenantDataGrantee == null ) {
			throw new ConfigurationException(
					"Oracle Deep Data Security row-level security requires both '"
							+ ORACLE_DEEP_DATA_SECURITY_TENANT_CONTEXT_NAME
							+ "' and '"
							+ ORACLE_DEEP_DATA_SECURITY_TENANT_DATA_GRANTEE
							+ "'"
			);
		}
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
				getBoolean( ORACLE_DEEP_DATA_SECURITY_USE_DATA_GRANTS_ONLY, settings, true )
		);
	}

	public OracleDeepDataSecurityRowLevelSecurity(
			String tenantContextName,
			String tenantIdentifierAttributeName,
			String rootTenantIdentifierAttributeName,
			String tenantDataGrantee,
			boolean useDataGrantsOnly) {
		this.tenantContextName = tenantContextName;
		this.tenantIdentifierAttributeName = tenantIdentifierAttributeName;
		this.rootTenantIdentifierAttributeName = rootTenantIdentifierAttributeName;
		this.tenantDataGrantee = tenantDataGrantee;
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
		if ( supportsRowLevelSecurity() ) {
			collector.getDatabase().addAuxiliaryDatabaseObject( new TenantContext( this ) );
			table.addInitCommand( context -> new InitCommand(
					getTenantIdTableCreateStrings(
							table,
							tenantIdentifierColumn,
							tenantIdentifierColumnType,
							context
					)
			) );
		}
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
				+ " where " + tenantPredicateWithRoot( tenantIdentifierColumn, tenantIdentifierColumnType, context )
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
	public void setTenantIdentifier(
			Connection connection,
			String tenantIdentifier,
			boolean root,
			ServiceRegistry serviceRegistry) throws SQLException {
		final Object baseContext = getContextProvider( serviceRegistry ).getEndUserSecurityContext();
		if ( baseContext == null ) {
			throw new SQLException( "Oracle Deep Data Security context provider returned null" );
		}

		final var oracleJdbc = OracleJdbcTypes.load();
		if ( !oracleJdbc.endUserSecurityContextClass.isInstance( baseContext ) ) {
			throw new SQLException(
					"Oracle Deep Data Security context provider must return an oracle.jdbc.EndUserSecurityContext"
			);
		}
		final Object oracleConnection = unwrapOracleConnection( connection, oracleJdbc.oracleConnectionClass );
		final Object tenantContext = tenantContext( oracleJdbc, baseContext, tenantIdentifier, root );
		invoke(
				oracleJdbc.oracleConnectionClass,
				oracleConnection,
				"setEndUserSecurityContext",
				new Class<?>[] { oracleJdbc.endUserSecurityContextClass },
				new Object[] { tenantContext }
		);
	}

	@Override
	public void clearTenantIdentifier(Connection connection, ServiceRegistry serviceRegistry) throws SQLException {
		final var oracleJdbc = OracleJdbcTypes.load();
		final Object oracleConnection = unwrapOracleConnection( connection, oracleJdbc.oracleConnectionClass );
		invoke( oracleJdbc.oracleConnectionClass, oracleConnection, "clearEndUserSecurityContext" );
	}

	@Override
	public String getTenantIdentifierSettingName() {
		return tenantContextName + "." + tenantIdentifierAttributeName;
	}

	@Override
	public String getRootTenantIdentifierSettingName() {
		return tenantContextName + "." + rootTenantIdentifierAttributeName;
	}

	String getTenantContextName() {
		return tenantContextName;
	}

	String getTenantIdentifierAttributeName() {
		return tenantIdentifierAttributeName;
	}

	String getRootTenantIdentifierAttributeName() {
		return rootTenantIdentifierAttributeName;
	}

	private OracleDeepDataSecurityContextProvider getContextProvider(ServiceRegistry serviceRegistry) throws SQLException {
		final Object providerReference = serviceRegistry.requireService( ConfigurationService.class )
				.getSettings()
				.get( ORACLE_DEEP_DATA_SECURITY_CONTEXT_PROVIDER );
		if ( providerReference == null ) {
			throw new SQLException(
					"Oracle Deep Data Security row-level security requires setting '"
							+ ORACLE_DEEP_DATA_SECURITY_CONTEXT_PROVIDER
							+ "' to an OracleDeepDataSecurityContextProvider"
			);
		}
		return serviceRegistry.requireService( StrategySelector.class )
				.resolveStrategy( OracleDeepDataSecurityContextProvider.class, providerReference );
	}

	private Object tenantContext(
			OracleJdbcTypes oracleJdbc,
			Object baseContext,
			String tenantIdentifier,
			boolean root) throws SQLException {
		final Object existingAttributes = invoke(
				oracleJdbc.endUserSecurityContextClass,
				baseContext,
				"attributes"
		);
		final Map<Object, Object> attributes = new HashMap<>();
		if ( existingAttributes instanceof Map<?, ?> existingAttributesMap ) {
			attributes.putAll( existingAttributesMap );
		}

		final Object factory = newInstance( oracleJdbc.oracleJsonFactoryClass );
		final Object existingTenantAttributes = attributes.get( tenantContextName );
		final Object tenantAttributes = existingTenantAttributes == null
				? invoke( oracleJdbc.oracleJsonFactoryClass, factory, "createObject" )
				: invoke(
						oracleJdbc.oracleJsonFactoryClass,
						factory,
						"createObject",
						new Class<?>[] { oracleJdbc.oracleJsonObjectClass },
						new Object[] { existingTenantAttributes }
				);
		invoke(
				oracleJdbc.oracleJsonObjectClass,
				tenantAttributes,
				"put",
				new Class<?>[] { String.class, String.class },
				new Object[] { tenantIdentifierAttributeName, tenantIdentifier }
		);
		invoke(
				oracleJdbc.oracleJsonObjectClass,
				tenantAttributes,
				"put",
				new Class<?>[] { String.class, String.class },
				new Object[] { rootTenantIdentifierAttributeName, root ? "true" : "false" }
		);
		attributes.put( tenantContextName, tenantAttributes );
		return invoke(
				oracleJdbc.endUserSecurityContextClass,
				baseContext,
				"withAttributes",
				new Class<?>[] { Map.class },
				new Object[] { attributes }
		);
	}

	private String tenantPredicateWithRoot(
			Column tenantIdentifierColumn,
			String tenantIdentifierColumnType,
			SqlStringGenerationContext context) {
		return tenantDiscriminatorPredicate( tenantIdentifierColumn, tenantIdentifierColumnType, context )
				+ " or " + endUserContextAttribute( rootTenantIdentifierAttributeName ) + " = 'true'";
	}

	private String tenantDiscriminatorPredicate(
			Column tenantIdentifierColumn,
			String tenantIdentifierColumnType,
			SqlStringGenerationContext context) {
		return tenantIdentifierColumn.getQuotedName( context.getDialect() )
				+ " = cast(" + endUserContextAttribute( tenantIdentifierAttributeName )
				+ " as " + tenantIdentifierColumnType + ")";
	}

	private String endUserContextAttribute(String attributeName) {
		return "ORA_END_USER_CONTEXT." + tenantContextName + "." + attributeName;
	}

	private String jsonSchema() {
		return "{\"type\":\"object\",\"properties\":{"
				+ "\"" + tenantIdentifierAttributeName + "\":{\"type\":\"string\"},"
				+ "\"" + rootTenantIdentifierAttributeName + "\":{\"type\":\"string\",\"default\":\"false\"}"
				+ "}}";
	}

	private static String dataGrantName(Table table) {
		return TENANT_ISOLATION_DATA_GRANT + "_" + Integer.toUnsignedString(
				table.getQualifiedTableName().toString().hashCode(),
				36
		);
	}

	private static String trimToNull(String value) {
		return isBlank( value ) ? null : value.trim();
	}

	private static String sqlStringLiteral(String value) {
		return "'" + value.replace( "'", "''" ) + "'";
	}

	private static Object unwrapOracleConnection(Connection connection, Class<?> oracleConnectionClass)
			throws SQLException {
		return oracleConnectionClass.isInstance( connection )
				? connection
				: connection.unwrap( oracleConnectionClass );
	}

	private static Object newInstance(Class<?> type) throws SQLException {
		try {
			return type.getConstructor().newInstance();
		}
		catch (ReflectiveOperationException e) {
			throw toSQLException( "Unable to instantiate " + type.getName(), e );
		}
	}

	private static Object invoke(Class<?> type, Object target, String methodName) throws SQLException {
		return invoke( type, target, methodName, new Class<?>[0], new Object[0] );
	}

	private static Object invoke(
			Class<?> type,
			Object target,
			String methodName,
			Class<?>[] parameterTypes,
			Object[] arguments) throws SQLException {
		try {
			return type.getMethod( methodName, parameterTypes ).invoke( target, arguments );
		}
		catch (ReflectiveOperationException e) {
			throw toSQLException( "Unable to invoke " + type.getName() + "." + methodName, e );
		}
	}

	private static SQLException toSQLException(String message, ReflectiveOperationException exception) {
		if ( exception instanceof InvocationTargetException invocationTargetException ) {
			final Throwable targetException = invocationTargetException.getTargetException();
			if ( targetException instanceof SQLException sqlException ) {
				return sqlException;
			}
			return new SQLException( message, targetException );
		}
		return new SQLException( message, exception );
	}

	private record TenantContext(OracleDeepDataSecurityRowLevelSecurity rowLevelSecurity)
			implements AuxiliaryDatabaseObject {

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
			return new String[] {
					"create or replace end user context " + rowLevelSecurity.getTenantContextName()
							+ " using json schema "
							+ sqlStringLiteral( rowLevelSecurity.jsonSchema() )
			};
		}

		@Override
		public String[] sqlDropStrings(SqlStringGenerationContext context) {
			return new String[] {
					"drop end user context " + rowLevelSecurity.getTenantContextName()
			};
		}

		@Override
		public String getExportIdentifier() {
			return "hibernate-row-level-security-oracle-deep-data-security-context-"
					+ rowLevelSecurity.getTenantContextName();
		}
	}

	private record OracleJdbcTypes(
			Class<?> oracleConnectionClass,
			Class<?> endUserSecurityContextClass,
			Class<?> oracleJsonFactoryClass,
			Class<?> oracleJsonObjectClass) {
		static OracleJdbcTypes load() throws SQLException {
			try {
				return new OracleJdbcTypes(
						Class.forName( "oracle.jdbc.OracleConnection" ),
						Class.forName( "oracle.jdbc.EndUserSecurityContext" ),
						Class.forName( "oracle.sql.json.OracleJsonFactory" ),
						Class.forName( "oracle.sql.json.OracleJsonObject" )
				);
			}
			catch (ClassNotFoundException e) {
				throw new SQLException(
						"Oracle Deep Data Security row-level security requires the Oracle JDBC driver",
						e
				);
			}
		}
	}
}
