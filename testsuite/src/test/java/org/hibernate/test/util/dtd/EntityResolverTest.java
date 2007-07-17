package org.hibernate.test.util.dtd;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.cfg.Configuration;
import org.hibernate.junit.UnitTestCase;


/**
 * @author Steve Ebersole
 */
public class EntityResolverTest extends UnitTestCase {

	public EntityResolverTest(String name) {
		super( name );
	}

	public static Test suite() {
		return new TestSuite( EntityResolverTest.class );
	}

	public void testEntityIncludeResolution() {
		// Parent.hbm.xml contains the following entity include:
		//		<!ENTITY child SYSTEM "classpath://org/hibernate/test/util/dtd/child.xml">
		// which we are expecting the Hibernate custom entity resolver to be able to resolve
		// locally via classpath lookup.
		Configuration cfg = new Configuration();
		cfg.addResource( "org/hibernate/test/util/dtd/Parent.hbm.xml" );
		cfg.buildMappings();
	}
}
