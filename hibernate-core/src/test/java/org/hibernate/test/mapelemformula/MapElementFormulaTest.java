/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.mapelemformula;

import org.junit.Test;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.FailureExpectedWithNewUnifiedXsd;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Gavin King
 */
@FailureExpectedWithNewUnifiedXsd(message = "formulas not yet supported in associations")
public class MapElementFormulaTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "mapelemformula/UserGroup.hbm.xml" };
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testManyToManyFormula() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User gavin = new User("gavin", "secret");
		User turin = new User("turin", "tiger");
		Group g = new Group("users");
		g.getUsers().put("Gavin", gavin);
		g.getUsers().put("Turin", turin);
		s.persist(g);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		g = (Group) s.get(Group.class, "users");
		assertEquals( g.getUsers().size(), 2 );
		g.getUsers().remove("Turin");
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		g = (Group) s.get(Group.class, "users");
		assertEquals( g.getUsers().size(), 1 );
		s.delete(g);
		s.delete( g.getUsers().get("Gavin") );
		s.delete( s.get(User.class, "turin") );
		t.commit();
		s.close();
	}

}

