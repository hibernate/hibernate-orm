//$Id$
package org.hibernate.test.annotations;

import org.hibernate.AnnotationException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;

/**
 * @author Emmanuel Bernard
 */
public class SafeMappingTest extends junit.framework.TestCase {
	public void testDeclarativeMix() throws Exception {
		AnnotationConfiguration cfg = new AnnotationConfiguration();
		cfg.addAnnotatedClass( IncorrectEntity.class );
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		try {
			SessionFactory sf = cfg.buildSessionFactory();
			fail( "Entity wo id should fail" );
		}
		catch (AnnotationException e) {
			//success
		}
	}
}
