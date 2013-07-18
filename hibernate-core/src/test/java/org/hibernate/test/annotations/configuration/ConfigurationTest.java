//$Id$
package org.hibernate.test.annotations.configuration;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestMethod;

/**
 * @author Emmanuel Bernard
 */
public class ConfigurationTest  extends BaseCoreFunctionalTestMethod{
    @Test
	public void testMixPackageAndResourceOrdering() throws Exception {
		try {
			getTestConfiguration().getOrmXmlFiles().add( "org/hibernate/test/annotations/configuration/orm.xml" );
			getTestConfiguration().getAnnotatedPackages().add(  "org.hibernate.test.annotations.configuration" );
			getSessionFactoryHelper().getSessionFactory();
		}
		catch( Exception e ) {
            Assert.fail( "Processing package first when ORM.xml is used should not fail" );
		}
	}
}
