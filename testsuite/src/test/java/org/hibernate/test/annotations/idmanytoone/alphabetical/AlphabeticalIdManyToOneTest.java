//$
package org.hibernate.test.annotations.idmanytoone.alphabetical;

import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class AlphabeticalIdManyToOneTest extends TestCase {
	public void testAlphabeticalTest() throws Exception {
		//test through deployment
	}


	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				B.class,
				C.class,
				A.class


		};
	}
}
