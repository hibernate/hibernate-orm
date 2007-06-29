/*
* Created on 06-Dec-2004
*
*/
package org.hibernate.test.mapping;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClassVisitor;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.junit.UnitTestCase;

/**
 * @author max
 * 
 */
public class PersistentClassVisitorTest extends UnitTestCase {

	public PersistentClassVisitorTest(String string) {
		super( string );
	}

	static public class PersistentClassVisitorValidator implements PersistentClassVisitor {

		private Object validate(Class expectedClass, Object visitee) {
			if (!visitee.getClass().getName().equals(expectedClass.getName())) {
				throw new IllegalStateException(visitee.getClass().getName()
						+ " did not call proper accept method. Was "
						+ expectedClass.getName());
			}
			return null;
		}

		public Object accept(RootClass class1) {
			return validate(RootClass.class, class1);
		}

		public Object accept(UnionSubclass subclass) {
			return validate(UnionSubclass.class, subclass);
		}

		public Object accept(SingleTableSubclass subclass) {
			return validate(SingleTableSubclass.class, subclass);
		}

		public Object accept(JoinedSubclass subclass) {
			return validate(JoinedSubclass.class, subclass);
		}

		public Object accept(Subclass subclass) {
			return validate(Subclass.class, subclass);
		}


	};

	public void testProperCallbacks() {

		PersistentClassVisitorValidator vv = new PersistentClassVisitorValidator();

		new RootClass().accept(vv);
		new Subclass(new RootClass()).accept(vv);
		new JoinedSubclass(new RootClass()).accept(vv);
		new SingleTableSubclass(new RootClass()).accept(vv);
		new UnionSubclass(new RootClass()).accept(vv);

	}

	public static Test suite() {
		return new TestSuite(PersistentClassVisitorTest.class);
	}

}
