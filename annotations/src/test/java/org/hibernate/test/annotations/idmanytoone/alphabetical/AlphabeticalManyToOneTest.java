//$
package org.hibernate.test.annotations.idmanytoone.alphabetical;

import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class AlphabeticalManyToOneTest extends TestCase {
	public void testAlphabeticalTest() throws Exception {
		//test through deployment
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Acces.class,
				Droitacces.class,
				Benefserv.class,
				Service.class
		};
	}
}
