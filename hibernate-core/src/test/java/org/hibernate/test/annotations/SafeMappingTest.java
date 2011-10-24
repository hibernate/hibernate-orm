//$Id$
package org.hibernate.test.annotations;
import org.junit.Test;

import org.hibernate.AnnotationException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;

import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class SafeMappingTest {
    @Test
	public void testDeclarativeMix() throws Exception {
		Configuration cfg = new Configuration();
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
