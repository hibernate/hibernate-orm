//$Id$
package org.hibernate.test.annotations.onetoone;

import org.hibernate.AnnotationException;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.spi.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;

/**
 * @author Emmanuel Bernard
 */
public class OneToOneErrorTest extends junit.framework.TestCase {
	public void testWrongOneToOne() throws Exception {
		AnnotationConfiguration cfg = new AnnotationConfiguration();
		cfg.addAnnotatedClass( Show.class )
				.addAnnotatedClass( ShowDescription.class );
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		ServiceRegistry serviceRegistry = null;
		try {
			serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
			cfg.buildSessionFactory( serviceRegistry );
			fail( "Wrong mappedBy does not fail property" );
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