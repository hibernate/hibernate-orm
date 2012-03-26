/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.ejb.internal.EntityManagerMessageLogger;
import org.hibernate.service.jdbc.connections.internal.DatasourceConnectionProviderImpl;

/**
 * A specialization of {@link DatasourceConnectionProviderImpl} which uses the {@link DataSource} specified vi
 * {@link #setDataSource} rather than locating it from JNDI.
 * <p/>
 * NOTE : {@link #setDataSource} must be called prior to {@link #configure}.
 * <p/>
 * TODO : could not find where #setDataSource is actually called.  Can't this just be passed in to #configure???
 *
 * @author Emmanuel Bernard
 */
public class InjectedDataSourceConnectionProvider extends DatasourceConnectionProviderImpl {

    private static final EntityManagerMessageLogger LOG = Logger.getMessageLogger(EntityManagerMessageLogger.class,
                                                                           InjectedDataSourceConnectionProvider.class.getName());

	private String user;
	private String pass;

	@Override
    public void setDataSource(DataSource ds) {
		super.setDataSource( ds );
	}

	public void configure(Properties props) throws HibernateException {
		user = props.getProperty( Environment.USER );
		pass = props.getProperty( Environment.PASS );

        if (getDataSource() == null) throw new HibernateException("No datasource provided");
        LOG.usingProvidedDataSource();
	}

	@Override
	public Connection getConnection() throws SQLException {
        if (user != null || pass != null) return getDataSource().getConnection(user, pass);
        return getDataSource().getConnection();
	}
}
