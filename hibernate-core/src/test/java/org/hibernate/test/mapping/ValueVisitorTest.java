/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2004-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.mapping;

import org.hibernate.cfg.NotYetImplementedException;
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
import org.hibernate.mapping.Set;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ValueVisitor;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author max
 */
public class ValueVisitorTest extends BaseUnitTestCase {
	@Test
	@FailureExpectedWithNewMetamodel
	public void testProperCallbacks() {
		throw new NotYetImplementedException(  );
//		final Mappings mappings = new Configuration().createMappings();
//		final Table tbl = new Table();
//		final RootClass rootClass = new RootClass();
//
//		ValueVisitor vv = new ValueVisitorValidator();
//
//		new Any( mappings, tbl ).accept(vv);
//		new Array( mappings, rootClass ).accept(vv);
//		new Bag( mappings, rootClass ).accept(vv);
//		new Component( mappings, rootClass ).accept(vv);
//		new DependantValue( mappings, tbl, null ).accept(vv);
//		new IdentifierBag( mappings, rootClass ).accept(vv);
//		new List( mappings, rootClass ).accept(vv);
//		new ManyToOne( mappings, tbl ).accept(vv);
//		new Map( mappings, rootClass ).accept(vv);
//		new OneToMany( mappings, rootClass ).accept(vv);
//		new OneToOne( mappings, tbl, rootClass ).accept(vv);
//		new PrimitiveArray( mappings, rootClass ).accept(vv);
//		new Set( mappings, rootClass ).accept(vv);
//		new SimpleValue( mappings ).accept(vv);
	}

	static public class ValueVisitorValidator implements ValueVisitor {

		public Object accept(PrimitiveArray primitiveArray) {
			return validate(PrimitiveArray.class,primitiveArray);
		}

		public Object accept(Bag bag) {
			return validate(Bag.class, bag);
		}

		public Object accept(DependantValue value) {
			return validate(DependantValue.class, value);
		}

		private Object validate(Class expectedClass, Object visitee) {
			if (!visitee.getClass().getName().equals(expectedClass.getName())) {
				throw new IllegalStateException(visitee.getClass().getName()
						+ " did not call proper accept method. Was "
						+ expectedClass.getName());
			}
			return null;
		}

		public Object accept(IdentifierBag bag) {
			return validate(IdentifierBag.class, bag);
		}

		public Object accept(List list) {
			return validate(List.class, list);
		}

		public Object accept(Map map) {
			return validate(Map.class, map);
		}

		public Object accept(Array list) {
			return validate(Array.class, list);
		}

		public Object accept(OneToMany many) {
			return validate(OneToMany.class, many);
		}

		public Object accept(Set set) {
			return validate(Set.class, set);
		}

		public Object accept(Any any) {
			return validate(Any.class, any);
		}

		public Object accept(SimpleValue value) {
			return validate(SimpleValue.class, value);
		}

		public Object accept(Component component) {
			return validate(Component.class, component);
		}

		public Object accept(ManyToOne mto) {
			return validate(ManyToOne.class, mto);
		}

		public Object accept(OneToOne oto) {
			return validate(OneToOne.class, oto);
		}

	}
}
