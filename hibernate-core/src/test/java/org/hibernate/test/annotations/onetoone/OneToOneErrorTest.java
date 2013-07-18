//$Id$
package org.hibernate.test.annotations.onetoone;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.AnnotationException;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestMethod;

/**
 * @author Emmanuel Bernard
 */
public class OneToOneErrorTest extends BaseCoreFunctionalTestMethod {
	@Test
	@FailureExpectedWithNewMetamodel
	public void testWrongOneToOne() throws Exception {
		getTestConfiguration().addAnnotatedClass( Show.class ).addAnnotatedClass( ShowDescription.class );
		try {
			getSessionFactoryHelper().getSessionFactory();
            Assert.fail( "Wrong mappedBy does not fail property" );
		}
		catch (AnnotationException e) {
			//success
		}
	}
}
