//$Id$
package org.hibernate.test.annotations.duplicatedgenerator;

import junit.framework.TestCase;

import org.hibernate.AnnotationException;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.spi.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;

/**
 * @author Emmanuel Bernard
 */
public class DuplicateTest extends TestCase {
	public void testDuplicateEntityName() throws Exception {
		AnnotationConfiguration cfg = new AnnotationConfiguration();
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		ServiceRegistry serviceRegistry = null;
		try {
			cfg.addAnnotatedClass( Flight.class );
			cfg.addAnnotatedClass( org.hibernate.test.annotations.Flight.class );
			cfg.addResource( "org/hibernate/test/annotations/orm.xml" );
			cfg.addResource( "org/hibernate/test/annotations/duplicatedgenerator/orm.xml" );
			serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( cfg.getProperties() );
			cfg.buildSessionFactory( serviceRegistry );
			fail( "Should not be able to map the same entity name twice" );
		}
		catch (AnnotationException ae) {
			//success
		}
		finally {
			if ( serviceRegistry != null ) {
				ServiceRegistryBuilder.destroy( serviceRegistry );
			}
		}
	}
}
