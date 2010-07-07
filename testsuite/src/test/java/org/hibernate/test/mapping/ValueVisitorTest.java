/*
 * Created on 06-Dec-2004
 *
 */
package org.hibernate.test.mapping;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Mappings;
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
import org.hibernate.testing.junit.UnitTestCase;

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
		final Mappings mappings = new Configuration().createMappings();
		final Table tbl = new Table();
		final RootClass rootClass = new RootClass();

		ValueVisitor vv = new ValueVisitorValidator();
		
		new Any( mappings, tbl ).accept(vv);
		new Array( mappings, rootClass ).accept(vv);
		new Bag( mappings, rootClass ).accept(vv);
		new Component( mappings, rootClass ).accept(vv);
		new DependantValue( mappings, tbl, null ).accept(vv);
		new IdentifierBag( mappings, rootClass ).accept(vv);
		new List( mappings, rootClass ).accept(vv);
		new ManyToOne( mappings, tbl ).accept(vv);
		new Map( mappings, rootClass ).accept(vv);
		new OneToMany( mappings, rootClass ).accept(vv);
		new OneToOne( mappings, tbl, rootClass ).accept(vv);
		new PrimitiveArray( mappings, rootClass ).accept(vv);
		new Set( mappings, rootClass ).accept(vv);
		new SimpleValue( mappings ).accept(vv);
	
		
	}

	public static Test suite() {
		return new TestSuite(ValueVisitorTest.class);
	}


}
