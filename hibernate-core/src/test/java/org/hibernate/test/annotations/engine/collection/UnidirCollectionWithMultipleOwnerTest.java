/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
