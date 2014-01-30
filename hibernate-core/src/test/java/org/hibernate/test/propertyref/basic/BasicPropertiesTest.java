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
package org.hibernate.test.propertyref.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
public class BasicPropertiesTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "propertyref/basic/EntityClass.hbm.xml" };
	}

	/**
	 * Really simple regression test for HHH-8689.
	 */
	@Test
	@TestForIssue(jiraKey = "HHH-8689")
	@FailureExpectedWithNewMetamodel
	public void testProperties() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		EntityClass ec = new EntityClass();
		ec.setKey( 1l );
		ec.setField1( "foo1" );
		ec.setField2( "foo2" );
		s.persist( ec );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		ec = (EntityClass) s.get( EntityClass.class, 1l );
		t.commit();
		s.close();
		
		assertNotNull( ec );
		assertEquals( ec.getField1(), "foo1" );
		assertEquals( ec.getField2(), "foo2" );
	}
}

