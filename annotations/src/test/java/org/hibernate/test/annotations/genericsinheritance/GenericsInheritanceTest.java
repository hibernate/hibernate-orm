//$Id$
package org.hibernate.test.annotations.genericsinheritance;

import org.hibernate.test.annotations.TestCase;
import org.hibernate.Session;

/**
 * @author Emmanuel Bernard
 */
public class GenericsInheritanceTest extends TestCase {
	public void testMapping() throws Exception {
		Session s = openSession();
		s.close();
		//mapping is tested
	}
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				ChildHierarchy1.class,
				ParentHierarchy1.class,
				ChildHierarchy22.class,
				ParentHierarchy22.class
		};
	}
}
