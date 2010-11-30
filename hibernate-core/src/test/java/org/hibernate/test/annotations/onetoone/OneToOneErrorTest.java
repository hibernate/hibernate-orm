//$Id$
package org.hibernate.test.annotations.onetoone;

import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.SessionFactory;
import org.hibernate.AnnotationException;
import org.hibernate.test.common.ServiceRegistryHolder;

/**
 * @author Emmanuel Bernard
 */
public class OneToOneErrorTest extends junit.framework.TestCase {
	public void testWrongOneToOne() throws Exception {
		AnnotationConfiguration cfg = new AnnotationConfiguration();
		cfg.addAnnotatedClass( Show.class )
				.addAnnotatedClass( ShowDescription.class );
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		ServiceRegistryHolder serviceRegistryHolder = null;
		try {
			serviceRegistryHolder = new ServiceRegistryHolder( Environment.getProperties() );
			SessionFactory sf = cfg.buildSessionFactory( serviceRegistryHolder.getServiceRegistry() );
			fail( "Wrong mappedBy does not fail property" );
		}
		catch (AnnotationException e) {
			//success
		}
		finally {
			if ( serviceRegistryHolder != null ) {
				serviceRegistryHolder.destroy();
			}			
		}
	}
}