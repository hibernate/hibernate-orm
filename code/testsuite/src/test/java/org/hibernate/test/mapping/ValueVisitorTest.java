/*
 * Created on 06-Dec-2004
 *
 */
package org.hibernate.test.mapping;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.mapping.Any;
import org.hibernate.mapping.Array;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.List;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PrimitiveArray;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Set;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ValueVisitor;
import org.hibernate.junit.UnitTestCase;

/**
 * @author max
 * 
 */
public class ValueVisitorTest extends UnitTestCase {

	public ValueVisitorTest(String string) {
		super( string );
	}

	static public class ValueVisitorValidator implements ValueVisitor {

		/* (non-Javadoc)
		 * @see org.hibernate.mapping.ValueVisitor#accept(org.hibernate.mapping.PrimitiveArray)
		 */
		public Object accept(PrimitiveArray primitiveArray) {
			return validate(PrimitiveArray.class,primitiveArray);
		}
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.hibernate.mapping.ValueVisitor#accept(org.hibernate.mapping.Bag)
		 */
		public Object accept(Bag bag) {
			return validate(Bag.class, bag);
		}

		/* (non-Javadoc)
		 * @see org.hibernate.mapping.ValueVisitor#accept(org.hibernate.mapping.DependantValue)
		 */
		public Object accept(DependantValue value) {
			return validate(DependantValue.class, value);
		}
		/**
		 * @param expectedClass
		 * @param visitee
		 */
		private Object validate(Class expectedClass, Object visitee) {
			if (!visitee.getClass().getName().equals(expectedClass.getName())) {
				throw new IllegalStateException(visitee.getClass().getName()
						+ " did not call proper accept method. Was "
						+ expectedClass.getName());
			}
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.hibernate.mapping.ValueVisitor#accept(org.hibernate.mapping.IdentifierBag)
		 */
		public Object accept(IdentifierBag bag) {
			return validate(IdentifierBag.class, bag);

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.hibernate.mapping.ValueVisitor#accept(org.hibernate.mapping.List)
		 */
		public Object accept(List list) {
			return validate(List.class, list);

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.hibernate.mapping.ValueVisitor#accept(org.hibernate.mapping.Map)
		 */
		public Object accept(Map map) {
			return validate(Map.class, map);

		}

		/* (non-Javadoc)
		 * @see org.hibernate.mapping.ValueVisitor#accept(org.hibernate.mapping.Array)
		 */
		public Object accept(Array list) {
			return validate(Array.class, list);
		}
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.hibernate.mapping.ValueVisitor#accept(org.hibernate.mapping.OneToMany)
		 */
		public Object accept(OneToMany many) {
			return validate(OneToMany.class, many);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.hibernate.mapping.ValueVisitor#accept(org.hibernate.mapping.Set)
		 */
		public Object accept(Set set) {
			return validate(Set.class, set);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.hibernate.mapping.ValueVisitor#accept(org.hibernate.mapping.Any)
		 */
		public Object accept(Any any) {
			return validate(Any.class, any);
			
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.hibernate.mapping.ValueVisitor#accept(org.hibernate.mapping.SimpleValue)
		 */
		public Object accept(SimpleValue value) {
			return validate(SimpleValue.class, value);

		}

		/* (non-Javadoc)
		 * @see org.hibernate.mapping.ValueVisitor#accept(org.hibernate.mapping.Component)
		 */
		public Object accept(Component component) {
			return validate(Component.class, component);
		}

		/* (non-Javadoc)
		 * @see org.hibernate.mapping.ValueVisitor#accept(org.hibernate.mapping.ManyToOne)
		 */
		public Object accept(ManyToOne mto) {
			return validate(ManyToOne.class, mto);
		}

		/* (non-Javadoc)
		 * @see org.hibernate.mapping.ValueVisitor#accept(org.hibernate.mapping.OneToOne)
		 */
		public Object accept(OneToOne oto) {
			return validate(OneToOne.class, oto);
		}

	};

	public void testProperCallbacks() {

		ValueVisitor vv = new ValueVisitorValidator();
		
		new Any(new Table()).accept(vv);
		new Array(new RootClass()).accept(vv);
		new Bag(new RootClass()).accept(vv);
		new Component(new RootClass()).accept(vv);
		new DependantValue(null,null).accept(vv);
		new IdentifierBag(null).accept(vv);
		new List(null).accept(vv);
		new ManyToOne(null).accept(vv);
		new Map(null).accept(vv);
		new OneToMany(null).accept(vv);
		new OneToOne(null, new RootClass() ).accept(vv);
		new PrimitiveArray(null).accept(vv);
		new Set(null).accept(vv);
		new SimpleValue().accept(vv);
	
		
	}

	public static Test suite() {
		return new TestSuite(ValueVisitorTest.class);
	}


}
