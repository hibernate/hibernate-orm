//$Id$
package org.hibernate.test.annotations.configuration;

import junit.framework.TestCase;
import org.hibernate.cfg.AnnotationConfiguration;

/**
 * @author Emmanuel Bernard
 */
public class ConfigurationTest extends TestCase {
	public void testMixPackageAndResourceOrdering() throws Exception {
		try {
			AnnotationConfiguration config = new AnnotationConfiguration();
			config.addResource( "org/hibernate/test/annotations/configuration/orm.xml" );
			config.addPackage( "org.hibernate.test.annotations.configuration" );
		}
		catch( Exception e ) {
			fail("Processing package first when ORM.xml is used should not fail");
		}
	}
}
