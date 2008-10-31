//$Id$
package org.hibernate.ejb.connection;

import java.util.Properties;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.connection.DatasourceConnectionProvider;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * A connection provider that uses an injected <tt>DataSource</tt>.
 * Setters has to be called before configure()
 *
 * @author Emmanuel Bernard
 * @see org.hibernate.connection.ConnectionProvider
 */
public class InjectedDataSourceConnectionProvider extends DatasourceConnectionProvider {
	private String user;
	private String pass;

	private final Logger log = LoggerFactory.getLogger( InjectedDataSourceConnectionProvider.class );

	public void setDataSource(DataSource ds) {
		super.setDataSource( ds );
	}

	public void configure(Properties props) throws HibernateException {
		user = props.getProperty( Environment.USER );
		pass = props.getProperty( Environment.PASS );

		if ( getDataSource() == null ) throw new HibernateException( "No datasource provided" );
		log.info( "Using provided datasource" );
	}

	@Override
	public Connection getConnection() throws SQLException {
		if (user != null || pass != null) {
			return getDataSource().getConnection(user, pass);
		}
		else {
			return getDataSource().getConnection();
		}
	}
}
