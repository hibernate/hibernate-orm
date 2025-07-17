/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.jdbc;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * A special connection provider that is shared across test runs for better performance.
 *
 * @author Christian Beikov
 * @author Loïc Lefèvre
 */
public class SharedDriverManagerConnectionProviderImpl extends DriverManagerConnectionProviderImpl {

	private static final SharedDriverManagerConnectionProviderImpl INSTANCE = new SharedDriverManagerConnectionProviderImpl();

	private static final Set<String> PGJDBC_STANDARD_TYPE_NAMES = buildTypeNames( Set.of(
			"int2",
			"int4",
			"oid",
			"int8",
			"money",
			"numeric",
			"float4",
			"float8",
			"char",
			"bpchar",
			"varchar",
			"text",
			"name",
			"bytea",
			"bool",
			"bit",
			"date",
			"time",
			"timetz",
			"timestamp",
			"timestamptz",
			"refcursor",
			"json",
			"jsonb",
			"box",
			"point",
			"uuid",
			"xml"
	) );

	private static Set<String> buildTypeNames(Set<String> baseTypeNames) {
		final HashSet<String> typeNames = new HashSet<>( baseTypeNames.size() * 3 );
		for ( String baseTypeName : baseTypeNames ) {
			typeNames.add( baseTypeName );
			typeNames.add( "_" + baseTypeName );
			typeNames.add( baseTypeName + "[]" );
		}
		return typeNames;
	}

	public static SharedDriverManagerConnectionProviderImpl getInstance() {
		return INSTANCE;
	}

	private Config config;
	private Boolean supportsIsValid;

	@Override
	public void configure(Map<String, Object> configurationValues) {
		final Config c = new Config( configurationValues );
		if ( !c.isCompatible( config ) ) {
			if ( config != null ) {
				super.stop();
			}
			super.configure( configurationValues );
			config = c;
		}
	}

	@Override
	public boolean isValid(Connection connection) throws SQLException {
		if ( supportsIsValid == Boolean.FALSE  ) {
			// Assume is valid if the driver doesn't support the check
			return true;
		}
		Boolean supportsIsValid = Boolean.FALSE;
		try {
			// Wait at most 5 seconds to validate a connection is still valid
			boolean valid = connection.isValid( 5 );
			supportsIsValid = Boolean.TRUE;
			return valid;
		}
		catch (AbstractMethodError e) {
			return true;
		}
		finally {
			this.supportsIsValid = supportsIsValid;
		}
	}

	@Override
	public void stop() {
		// No need to stop as this is a shared instance
		validateConnectionsReturned();
	}

	public void clearTypeCache() {
		if ( "oracle.jdbc.OracleDriver".equals( config.driverClassName ) ) {
			validateConnections( c -> {
				try {
					final Class<?> oracleConnection = Class.forName( "oracle.jdbc.OracleConnection" );
					final Object connection = c.unwrap( oracleConnection );
					oracleConnection.getMethod( "removeAllDescriptor").invoke( connection );
					return true;
				}
				catch (Exception e) {
					throw new RuntimeException( e );
				}
			} );
		}
		else if ( "org.postgresql.Driver".equals( config.driverClassName ) ) {
			validateConnections( c -> {
				// Until pgjdbc provides a method for this out of the box, we have to do this manually
				// See https://github.com/pgjdbc/pgjdbc/issues/3049
				try {
					final Class<?> pgConnection = Class.forName( "org.postgresql.jdbc.PgConnection" );
					final Object connection = c.unwrap( pgConnection );
					final Object typeInfo = pgConnection.getMethod( "getTypeInfo" ).invoke( connection );
					final Class<?> typeInfoCacheClass = Class.forName( "org.postgresql.jdbc.TypeInfoCache" );
					final Field oidToPgNameField = typeInfoCacheClass.getDeclaredField( "oidToPgName" );
					final Field pgNameToOidField = typeInfoCacheClass.getDeclaredField( "pgNameToOid" );
					final Field pgNameToSQLTypeField = typeInfoCacheClass.getDeclaredField( "pgNameToSQLType" );
					final Field oidToSQLTypeField = typeInfoCacheClass.getDeclaredField( "oidToSQLType" );
					oidToPgNameField.setAccessible( true );
					pgNameToOidField.setAccessible( true );
					pgNameToSQLTypeField.setAccessible( true );
					oidToSQLTypeField.setAccessible( true );
					//noinspection unchecked
					final Map<Integer, String> oidToPgName = (Map<Integer, String>) oidToPgNameField.get( typeInfo );
					//noinspection unchecked
					final Map<String, Integer> pgNameToOid = (Map<String, Integer>) pgNameToOidField.get( typeInfo );
					//noinspection unchecked
					final Map<String, Integer> pgNameToSQLType = (Map<String, Integer>) pgNameToSQLTypeField.get( typeInfo );
					//noinspection unchecked
					final Map<Integer, Integer> oidToSQLType = (Map<Integer, Integer>) oidToSQLTypeField.get( typeInfo );
					for ( Iterator<Map.Entry<String, Integer>> iter = pgNameToOid.entrySet().iterator(); iter.hasNext(); ) {
						Map.Entry<String, Integer> entry = iter.next();
						final String typeName = entry.getKey();
						if ( !PGJDBC_STANDARD_TYPE_NAMES.contains( typeName ) ) {
							final Integer oid = entry.getValue();
							oidToPgName.remove( oid );
							oidToSQLType.remove( oid );
							pgNameToSQLType.remove( typeName );
							iter.remove();
						}
					}
					return true;
				}
				catch (Exception e) {
					throw new RuntimeException( e );
				}
			} );
		}
		else if ( "com.edb.Driver".equals( config.driverClassName ) ) {
			validateConnections( c -> {
				// Until pgjdbc provides a method for this out of the box, we have to do this manually
				// See https://github.com/pgjdbc/pgjdbc/issues/3049
				try {
					final Class<?> pgConnection = Class.forName( "com.edb.jdbc.PgConnection" );
					final Object connection = c.unwrap( pgConnection );
					final Object typeInfo = pgConnection.getMethod( "getTypeInfo" ).invoke( connection );
					final Class<?> typeInfoCacheClass = Class.forName( "com.edb.jdbc.TypeInfoCache" );
					final Field oidToPgNameField = typeInfoCacheClass.getDeclaredField( "oidToPgName" );
					final Field pgNameToOidField = typeInfoCacheClass.getDeclaredField( "pgNameToOid" );
					final Field pgNameToSQLTypeField = typeInfoCacheClass.getDeclaredField( "pgNameToSQLType" );
					final Field oidToSQLTypeField = typeInfoCacheClass.getDeclaredField( "oidToSQLType" );
					oidToPgNameField.setAccessible( true );
					pgNameToOidField.setAccessible( true );
					pgNameToSQLTypeField.setAccessible( true );
					oidToSQLTypeField.setAccessible( true );
					//noinspection unchecked
					final Map<Integer, String> oidToPgName = (Map<Integer, String>) oidToPgNameField.get( typeInfo );
					//noinspection unchecked
					final Map<String, Integer> pgNameToOid = (Map<String, Integer>) pgNameToOidField.get( typeInfo );
					//noinspection unchecked
					final Map<String, Integer> pgNameToSQLType = (Map<String, Integer>) pgNameToSQLTypeField.get( typeInfo );
					//noinspection unchecked
					final Map<Integer, Integer> oidToSQLType = (Map<Integer, Integer>) oidToSQLTypeField.get( typeInfo );
					for ( Iterator<Map.Entry<String, Integer>> iter = pgNameToOid.entrySet().iterator(); iter.hasNext(); ) {
						Map.Entry<String, Integer> entry = iter.next();
						final String typeName = entry.getKey();
						if ( !PGJDBC_STANDARD_TYPE_NAMES.contains( typeName ) ) {
							final Integer oid = entry.getValue();
							oidToPgName.remove( oid );
							oidToSQLType.remove( oid );
							pgNameToSQLType.remove( typeName );
							iter.remove();
						}
					}
					return true;
				}
				catch (Exception e) {
					throw new RuntimeException( e );
				}
			} );
		}
	}

	public void onDefaultTimeZoneChange() {
		if ( "org.h2.Driver".equals( config.driverClassName ) || "org.hsqldb.jdbc.JDBCDriver".equals( config.driverClassName ) ) {
			// Clear the connection pool to avoid issues with drivers that initialize the session TZ to the system TZ
			super.stop();
		}
	}

	public void reset() {
		super.stop();
	}

	@Override
	public int getOpenConnections() {
		return super.getOpenConnections();
	}

	private static class Config {
		private final boolean autoCommit;
		private final int minSize;
		private final int maxSize;
		private final int initialSize;
		private final String driverClassName;
		private final String url;
		private final Properties connectionProps;
		private final Integer isolation;

		public Config(Map<String,Object> configurationValues) {
			this.autoCommit = ConfigurationHelper.getBoolean( AvailableSettings.AUTOCOMMIT, configurationValues );
			this.minSize = ConfigurationHelper.getInt( MIN_SIZE, configurationValues, 0 );
			this.maxSize = ConfigurationHelper.getInt( AvailableSettings.POOL_SIZE, configurationValues, 20 );
			this.initialSize = ConfigurationHelper.getInt( INITIAL_SIZE, configurationValues, minSize );
			this.driverClassName = (String) configurationValues.get( AvailableSettings.DRIVER );
			this.url = (String) configurationValues.get( AvailableSettings.URL );
			this.connectionProps = ConnectionProviderInitiator.getConnectionProperties( configurationValues );
			this.isolation = ConnectionProviderInitiator.extractIsolation( configurationValues );
		}

		boolean isCompatible(Config config) {
			return config != null && autoCommit == config.autoCommit && minSize == config.minSize
					&& maxSize == config.maxSize && initialSize == config.initialSize
					&& driverClassName.equals( config.driverClassName )
					&& url.equals( config.url )
					&& connectionProps.equals( config.connectionProps )
					&& Objects.equals( isolation, config.isolation );
		}

	}
}
