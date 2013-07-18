//$Id$
package org.hibernate.test.annotations.duplicatedgenerator;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestMethod;

/**
 * @author Emmanuel Bernard
 */
public class DuplicateTest extends BaseCoreFunctionalTestMethod {
	@Test
	public void testDuplicateEntityName() throws Exception {
		try {

			getTestConfiguration().addAnnotatedClass( Flight.class )
					.addAnnotatedClass( org.hibernate.test.annotations.Flight.class )
					.addOrmXmlFile( "org/hibernate/test/annotations/orm.xml" )
					.addOrmXmlFile( "org/hibernate/test/annotations/duplicatedgenerator/orm.xml" );
			getSessionFactoryHelper().getSessionFactory();
			Assert.fail( "Should not be able to map the same entity name twice" );
		}
		catch ( AnnotationException ae ) {
			//success
			//todo we have mismatch behavior here, the old metamodel thorws this exception, but the new metamodel throws MappingException.
		}
		catch ( MappingException me ) {
			//success
		}

	}
}
