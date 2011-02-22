//$Id$
package org.hibernate.test.annotations;

import org.hibernate.AnnotationException;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.spi.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;

/**
 * @author Emmanuel Bernard
 */
public class SafeMappingTest extends junit.framework.TestCase {
	public void testDeclarativeMix() throws Exception {
		AnnotationConfiguration cfg = new AnnotationConfiguration();
		cfg.addAnnotatedClass( IncorrectEntity.class );
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		ServiceRegistry serviceRegistry = null;
		try {
			serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( cfg.getProperties() );
			cfg.buildSessionFactory( serviceRegistry );
			fail( "Entity wo id should fail" );
		}
		catch (AnnotationException e) {
			//success
		}
		finally {
			if ( serviceRegistry != null ) {
				ServiceRegistryBuilder.destroy( serviceRegistry );
			}
		}
	}
}
