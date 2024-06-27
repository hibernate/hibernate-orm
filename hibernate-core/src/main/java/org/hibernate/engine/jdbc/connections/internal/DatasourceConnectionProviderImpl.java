/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.InjectService;
import org.hibernate.service.spi.Stoppable;

/**
 * A {@link ConnectionProvider} that manages connections from an underlying {@link DataSource}.
 * <p>
 * The {@link DataSource} to use may be specified by either:<ul>
 * <li>injection using {@link #setDataSource}
 * <li>declaring the {@link DataSource} instance using the {@value Environment#DATASOURCE} config property
 * <li>declaring the JNDI name under which the {@link DataSource} is found via the {@value Environment#DATASOURCE} config property
 * </ul>
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DatasourceConnectionProviderImpl implements ConnectionProvider, Configurable, Stoppable {
	private DataSource dataSource;
	private String user;
	private String pass;
	private boolean useCredentials;
	private JndiService jndiService;

	private boolean available;

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@InjectService( required = false )
	@SuppressWarnings("UnusedDeclaration")
	public void setJndiService(JndiService jndiService) {
		this.jndiService = jndiService;
	}

	@Override
	public boolean isUnwrappableAs(Class<?> unwrapType) {
		return ConnectionProvider.class.equals( unwrapType )
			|| DatasourceConnectionProviderImpl.class.isAssignableFrom( unwrapType )
			|| DataSource.class.isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public <T> T unwrap(Class<T> unwrapType) {
		if ( ConnectionProvider.class.equals( unwrapType )
				|| DatasourceConnectionProviderImpl.class.isAssignableFrom( unwrapType ) ) {
			return (T) this;
		}
		else if ( DataSource.class.isAssignableFrom( unwrapType ) ) {
			return (T) getDataSource();
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}

	@Override
	public void configure(Map<String, Object> configValues) {
		if ( dataSource == null ) {
			final Object dataSourceSetting = configValues.get( Environment.DATASOURCE );
			if ( dataSourceSetting instanceof DataSource ) {
				dataSource = (DataSource) dataSourceSetting;
			}
			else {
				final String dataSourceJndiName = (String) dataSourceSetting;
				if ( dataSourceJndiName == null ) {
					throw new HibernateException(
							"DataSource to use was not injected nor specified by [" + Environment.DATASOURCE
									+ "] configuration property"
					);
				}
				if ( jndiService == null ) {
					throw new HibernateException( "Unable to locate JndiService to lookup Datasource" );
				}
				dataSource = (DataSource) jndiService.locate( dataSourceJndiName );
			}
		}
		if ( dataSource == null ) {
			throw new HibernateException( "Unable to determine appropriate DataSource to use" );
		}

		user = (String) configValues.get( Environment.USER );
		pass = (String) configValues.get( Environment.PASS );
		useCredentials = user != null || pass != null;
		available = true;
	}

	@Override
	public void stop() {
		available = false;
		dataSource = null;
	}

	@Override
	public Connection getConnection() throws SQLException {
		if ( !available ) {
			throw new HibernateException( "Provider is closed" );
		}
		return useCredentials ? dataSource.getConnection( user, pass ) : dataSource.getConnection();
	}

	@Override
	public void closeConnection(Connection connection) throws SQLException {
		connection.close();
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return true;
	}
}
