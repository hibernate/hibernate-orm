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
package org.hibernate.test.annotations.entitynonentity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class EntityNonEntityTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testMix() throws Exception {
		GSM gsm = new GSM();
		gsm.brand = "Sony";
		gsm.frequency = 900;
		gsm.isNumeric = true;
		gsm.number = 2;
		gsm.species = "human";
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( gsm );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		gsm = (GSM) s.get( GSM.class, gsm.id );
		assertEquals( "top mapped superclass", 2, gsm.number );
		assertNull( "non entity between mapped superclass and entity", gsm.species );
		assertTrue( "mapped superclass under entity", gsm.isNumeric );
		assertNull( "non entity under entity", gsm.brand );
		assertEquals( "leaf entity", 900, gsm.frequency );
		s.delete( gsm );
		tx.commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Phone.class,
				Voice.class,
				GSM.class
		};
	}
}
