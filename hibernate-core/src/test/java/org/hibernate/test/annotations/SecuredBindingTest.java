/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations;

import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class SecuredBindingTest {


     @Test
	public void testConfigurationMethods() throws Exception {
		Configuration ac = new Configuration();
		Properties p = new Properties();
		p.put( Environment.DIALECT, "org.hibernate.dialect.HSQLDialect" );
		p.put( "hibernate.connection.driver_class", "org.hsqldb.jdbcDrive" );
		p.put( "hibernate.connection.url", "jdbc:hsqldb:." );
		p.put( "hibernate.connection.username", "sa" );
		p.put( "hibernate.connection.password", "" );
		p.put( "hibernate.show_sql", "true" );
		ac.setProperties( p );
		ac.addAnnotatedClass( Plane.class );
		SessionFactory sf=null;
		ServiceRegistry serviceRegistry = null;
		try {
			serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( p );
			sf = ac.buildSessionFactory( serviceRegistry );
			try {
				sf.close();
			}
			catch (Exception ignore) {
			}
            Assert.fail( "Driver property overriding should work" );
		}
		catch (HibernateException he) {
			//success
		}
		finally {
			if(sf!=null){
				sf.close();
			}
			if ( serviceRegistry != null ) {
				ServiceRegistryBuilder.destroy( serviceRegistry );
			}
		}
	}
}

