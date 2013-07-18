//$Id$
package org.hibernate.test.annotations;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestMethod;

/**
 * @author Emmanuel Bernard
 */
public class SecuredBindingTest extends BaseCoreFunctionalTestMethod {


	@Test
	public void testConfigurationMethods() throws Exception {
		Properties p = new Properties();
		p.put( Environment.DIALECT, "org.hibernate.dialect.HSQLDialect" );
		p.put( "hibernate.connection.driver_class", "org.hsqldb.jdbcDrive" );
		p.put( "hibernate.connection.url", "jdbc:hsqldb:." );
		p.put( "hibernate.connection.username", "sa" );
		p.put( "hibernate.connection.password", "" );
		p.put( "hibernate.show_sql", "true" );

		getTestConfiguration().getProperties().putAll( p );
		getTestConfiguration().addAnnotatedClass( Plane.class );
		try {
			getSessionFactoryHelper().getSessionFactory();
			Assert.fail( "Driver property overriding should work" );
		}
		catch ( HibernateException he ) {
			//success
		}

	}
}

