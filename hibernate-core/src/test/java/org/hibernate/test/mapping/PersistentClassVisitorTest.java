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

import org.junit.Test;

import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClassVisitor;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * Simple smoke style tests to make sure visitors keep working.
 * 
 * @author max
 */
public class PersistentClassVisitorTest extends BaseUnitTestCase {
	@Test
	public void testProperCallbacks() {
		PersistentClassVisitorValidator vv = new PersistentClassVisitorValidator();
		new RootClass().accept( vv );
		new Subclass( new RootClass() ).accept( vv );
		new JoinedSubclass( new RootClass() ).accept( vv );
		new SingleTableSubclass( new RootClass() ).accept( vv );
		new UnionSubclass( new RootClass() ).accept( vv );
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
	}

}
