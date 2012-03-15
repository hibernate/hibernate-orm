/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.test.annotations.xml.ejb3;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

@TestForIssue(jiraKey = "HHH-6271")
public class OrmVersion1SupportedTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testOrm1Support() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Light light = new Light();
		light.name = "the light at the end of the tunnel";
		s.persist( light );
		s.flush();
		s.clear();

		assertEquals( 1, s.getNamedQuery( "find.the.light" ).list().size() );
		tx.rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Light.class };
	}

	@Override
	protected String[] getXmlFiles() {
		return new String[] { "org/hibernate/test/annotations/xml/ejb3/orm2.xml" };
	}
}
