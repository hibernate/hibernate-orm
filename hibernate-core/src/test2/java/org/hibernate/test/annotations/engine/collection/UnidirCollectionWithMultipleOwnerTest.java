/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.engine.collection;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Emmanuel Bernard
 */
public class UnidirCollectionWithMultipleOwnerTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testUnidirCollectionWithMultipleOwner() throws Exception {
		Session s = openSession();
		Transaction tx;
		tx = s.beginTransaction();
		Father father = new Father();
		Mother mother = new Mother();
		s.save( father );
		//s.save( mother );
		Son son = new Son();
		father.getOrderedSons().add( son );
		son.setFather( father );
		mother.getSons().add( son );
		son.setMother( mother );
		s.save( mother );
		s.save( father );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();
		son = (Son) s.get( Son.class, son.getId() );
		s.delete( son );
		s.flush();
		father = (Father) s.get( Father.class, father.getId() );
		mother = (Mother) s.get( Mother.class, mother.getId() );
		s.delete( father );
		s.delete( mother );
		tx.commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Father.class,
				Mother.class,
				Son.class
		};
	}
}
