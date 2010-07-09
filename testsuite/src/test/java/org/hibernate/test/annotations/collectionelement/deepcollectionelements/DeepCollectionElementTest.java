//$
package org.hibernate.test.annotations.collectionelement.deepcollectionelements;

import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */

//TEST not used: wait for ANN-591 and HHH-3157 
public class DeepCollectionElementTest extends TestCase {

	public void testInitialization() throws Exception {
		//test only the SF creation

	}

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				//A.class,
				//B.class,
				//C.class
		};
	}
}
