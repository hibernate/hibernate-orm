//$Id$
package org.hibernate.test.annotations;

import java.util.Properties;

import junit.framework.TestCase;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;

/**
 * @author Emmanuel Bernard
 */
public class SecuredBindingTest extends TestCase {

	public SecuredBindingTest(String x) {
		super( x );
	}

	public void testConfigurationMethods() throws Exception {
		AnnotationConfiguration ac = new AnnotationConfiguration();
		Properties p = new Properties();
		p.put( Environment.DIALECT, "org.hibernate.dialect.HSQLDialect" );
		p.put( "hibernate.connection.driver_class", "org.hsqldb.jdbcDrive" );
		p.put( "hibernate.connection.url", "jdbc:hsqldb:." );
		p.put( "hibernate.connection.username", "sa" );
		p.put( "hibernate.connection.password", "" );
		p.put( "hibernate.show_sql", "true" );
		ac.setProperties( p );
		ac.addAnnotatedClass( Plane.class );
		SessionFactory sf;
		try {
			sf = ac.buildSessionFactory();
			fail( "Driver property overriding should work" );
			sf.close();
		}
		catch (HibernateException he) {
			//success
		}

	}
}

