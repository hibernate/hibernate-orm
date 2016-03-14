/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.hql;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.junit.Test;

/**
 * This test case contains two simple tests for demonstrating the patch applied to hibernate 3.6.1.<br/>
 * One test method should always work, the other one will only work with a patched hibernate version.
 *
 * @author Oleksandr Dukhno
 */
public class InsertWithSubSelectTest extends BaseCoreFunctionalTestCase {

	@Override
	public String[] getMappings() {
		return new String[] {"hql/A.hbm.xml"};
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5274")
	public void testInsert() throws Exception {
		Session s;
		Transaction tx;

		s = openSession();
		tx = s.beginTransaction();
		String hql = "insert into C (id) select a.id from A a where exists (select 1 from B b where b.id = a.id)";
		Query query = s.createQuery( hql );
		query.executeUpdate();
		tx.commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5274")
	public void testSelect() throws Exception {
		Session s;
		Transaction tx;

		s = openSession();
		tx = s.beginTransaction();
		String hql = "select a.id from A a where exists (select 1 from B b where b.id = a.id)";
		Query query = s.createQuery( hql );
		query.list();
		tx.commit();
		s.close();
	}
}
