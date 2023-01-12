/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Database;
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

	public static boolean USER_CREATED = false;
	private static final SharedDriverManagerConnectionProviderImpl INSTANCE = new SharedDriverManagerConnectionProviderImpl();

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
			if(c.url.startsWith(Database.ORACLE.getUrlPrefix())) {
				try {
					final String oldUserName = (String) configurationValues.get(AvailableSettings.USER);
					final String password = (String) configurationValues.get(AvailableSettings.PASS);
					final String newUserName = oldUserName + "_" + ProcessHandle.current().pid();

					if(!USER_CREATED) {
						final Properties props = new Properties();
						props.put("user",oldUserName);
						props.put("password",password);
						try (Connection con = DriverManager.getConnection(c.url, props)) {
							try (Statement s = con.createStatement()) {
								s.execute("create user "+newUserName+" identified by \""+password+"\" quota unlimited on users");
								s.execute("grant all privileges to "+newUserName);
								USER_CREATED = true;
							}
						}
					}
					configurationValues.put(AvailableSettings.USER,newUserName);
				}
				catch (SQLException e) {
					throw new IllegalStateException( (String) configurationValues.get(AvailableSettings.USER)+" / "+(String) configurationValues.get(AvailableSettings.PASS) ,e );
				}
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

	public void reset() {
		super.stop();
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
			this.autoCommit = ConfigurationHelper.getBoolean( AvailableSettings.AUTOCOMMIT, configurationValues, false );
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
