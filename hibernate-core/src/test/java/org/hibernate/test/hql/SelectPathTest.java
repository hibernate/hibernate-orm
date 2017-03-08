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
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Test cases for various query with a path in the select clause.
 *
 * @author Etienne Miret
 */
public class SelectPathTest extends BaseCoreFunctionalTestCase {

	@Override
	public String[] getMappings() {
		return new String[] { "hql/Animal.hbm.xml" };
	}

	@Test
	public void subtypePathTest() {
		final Session s = openSession();
		final Query q = s.createQuery( "select h.nickName from Animal h" );
		q.list();
		s.close();
	}

	@Test
	public void joinedPathTest() {
		final Session s = openSession();
		final Query q = s.createQuery( "select h.mother.description from Animal h" );
		q.list();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9347" )
	@FailureExpected( jiraKey = "HHH-9347" )
	public void joinedSubtypePathTest() {
		final Session s = openSession();
		final Query q = s.createQuery( "select h.mother.nickName from Animal h" );
		q.list();
		s.close();
	}

}
