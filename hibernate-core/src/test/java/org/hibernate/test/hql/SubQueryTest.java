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

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.Session;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * NOTE : some subquery related tests still exist in other test classes in the suite.  This is a later
 * attempt to create a more targeted set of subquery related tests.
 *
 * @author Steve Ebersole
 */
public class SubQueryTest extends BaseCoreFunctionalTestCase {

	@Entity( name = "Root" )
	@Table( name = "ROOT" )
	public static class Root {
		@Id
		public Integer id;
		public String rootName;
		@OneToOne
		public Branch branch;
	}


	@Entity( name = "Branch" )
	@Table( name = "BRANCH" )
	public static class Branch {
		@Id
		public Integer id;
		public String branchName;

		@OneToMany
		public Set<Leaf> leaves;
	}

	@Entity( name = "Leaf" )
	@Table( name = "LEAF" )
	public static class Leaf {
		@Id
		public Integer id;
		public String leafName;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Root.class, Branch.class, Leaf.class };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9090" )
	@FailureExpected( jiraKey = "HHH-9090" )
	public void testCorrelatedJoin() {
		Session s = openSession();
		s.beginTransaction();

		// simple syntax check of the generated SQL
		final String qry = "from Root as r " +
				"where r.branch.branchName = 'some branch name' " +
				"  and exists( from r.branch.leaves as s where s.leafName = 'some leaf name')";
		s.createQuery( qry ).list();

		s.getTransaction().commit();
		s.close();
	}
}
