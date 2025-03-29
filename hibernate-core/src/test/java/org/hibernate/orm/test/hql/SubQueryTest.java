/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.Session;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
		@GeneratedValue
		public Integer id;
		public String rootName;
		@OneToOne
		public Branch branch;
	}


	@Entity( name = "Branch" )
	@Table( name = "BRANCH" )
	public static class Branch {
		@Id
		@GeneratedValue
		public Integer id;
		public String branchName;

		@OneToMany
		public List<Leaf> leaves;
	}

	@Entity( name = "Leaf" )
	@Table( name = "LEAF" )
	public static class Leaf {
		@Id
		@GeneratedValue
		public Integer id;
		public String leafName;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Root.class, Branch.class, Leaf.class };
	}

	@Test
	@JiraKey( value = "HHH-9090" )
	public void testCorrelatedJoin() {
		Session s = openSession();
		s.beginTransaction();
		Root root = new Root();
		root.rootName = "root name";
		root.branch = new Branch();
		root.branch.branchName = "branch";
		root.branch.leaves = new ArrayList<Leaf>();
		Leaf leaf1 = new Leaf();
		leaf1.leafName = "leaf1";
		Leaf leaf2 = new Leaf();
		leaf2.leafName = "leaf2";
		root.branch.leaves.add( leaf1 );
		root.branch.leaves.add( leaf2 );
		s.persist( leaf1 );
		s.persist( leaf2 );
		s.persist( root.branch );
		s.persist( root );
		Root otherRoot = new Root();
		otherRoot.rootName = "other root name";
		otherRoot.branch = new Branch();
		otherRoot.branch.branchName = "other branch";
		otherRoot.branch.leaves = new ArrayList<Leaf>();
		Leaf otherLeaf1 = new Leaf();
		otherLeaf1.leafName = "leaf1";
		Leaf otherLeaf3 = new Leaf();
		otherLeaf3.leafName = "leaf3";
		otherRoot.branch.leaves.add( otherLeaf1 );
		otherRoot.branch.leaves.add( otherLeaf3 );
		s.persist( otherLeaf1 );
		s.persist( otherLeaf3 );
		s.persist( otherRoot.branch );
		s.persist( otherRoot );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		String qry = "from Root as r " +
				"where r.branch.branchName = 'branch' " +
				"  and exists( from r.branch.leaves as s where s.leafName = 'leaf1')";
		Root rootQueried = s.createQuery( qry, Root.class ).uniqueResult();
		assertEquals( root.rootName, rootQueried.rootName );
		assertEquals( root.branch.branchName, rootQueried.branch.branchName );
		assertEquals( leaf1.leafName, rootQueried.branch.leaves.get( 0 ).leafName );
		assertEquals( leaf2.leafName, rootQueried.branch.leaves.get( 1 ).leafName );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		qry = "from Root as r " +
				"where r.branch.branchName = 'branch' " +
				"  and exists( from r.branch.leaves as s where s.leafName = 'leaf3')";
		assertNull( s.createQuery( qry ).uniqueResult() );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		qry = "from Root as r " +
				"where exists( from r.branch.leaves as s where r.branch.branchName = 'branch' and s.leafName = 'leaf1')";
		rootQueried = (Root) s.createQuery( qry ).uniqueResult();
		assertEquals( root.rootName, rootQueried.rootName );
		assertEquals( root.branch.branchName, rootQueried.branch.branchName );
		assertEquals( leaf1.leafName, rootQueried.branch.leaves.get( 0 ).leafName );
		assertEquals( leaf2.leafName, rootQueried.branch.leaves.get( 1 ).leafName );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		qry = "from Root as r" +
				" where exists( from Root r1 where r1.branch.branchName = r.branch.branchName and r1.branch.branchName != 'other branch')";
		rootQueried = (Root) s.createQuery( qry ).uniqueResult();
		assertEquals( root.rootName, rootQueried.rootName );
		assertEquals( root.branch.branchName, rootQueried.branch.branchName );
		assertEquals( leaf1.leafName, rootQueried.branch.leaves.get( 0 ).leafName );
		assertEquals( leaf2.leafName, rootQueried.branch.leaves.get( 1 ).leafName );
		s.getTransaction().commit();
		s.close();

	}

	@Test
	@JiraKeyGroup( value = {
			@JiraKey( value = "HHH-1689" ),
			@JiraKey( value = "SQM-30" )
	} )
	public void testSubQueryAsSearchedCaseResultExpression() {
		final String query = "SELECT CASE WHEN l.id IS NOT NULL THEN (SELECT COUNT(r.id) FROM Root r) ELSE 0 END FROM Leaf l";
		// simple syntax check
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( query ).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@JiraKeyGroup( value = {
			@JiraKey( value = "HHH-1689" ),
			@JiraKey( value = "SQM-30" )
	} )
	public void testSubQueryAsSearchedCaseExpression() {
		final String query = "SELECT CASE WHEN (SELECT COUNT(r.id) FROM Root r) > 1 THEN 1 ELSE 0 END FROM Leaf l";
		// simple syntax check
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( query ).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@JiraKeyGroup( value = {
			@JiraKey( value = "HHH-1689" ),
			@JiraKey( value = "SQM-30" )
	} )
	public void testSubQueryAsCaseElseResultExpression() {
		final String query = "SELECT CASE WHEN  l.id > 1 THEN 1 ELSE (SELECT COUNT(r.id) FROM Root r) END FROM Leaf l";
		// simple syntax check
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( query ).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@JiraKeyGroup( value = {
			@JiraKey( value = "HHH-1689" ),
			@JiraKey( value = "SQM-30" )
	} )
	public void testSubQueryAsSimpleCaseTestExpression() {
		final String query = "SELECT CASE (SELECT COUNT(r.id) FROM Root r) WHEN  1 THEN 1 ELSE 0 END FROM Leaf l";
		// simple syntax check
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( query ).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@JiraKeyGroup( value = {
			@JiraKey( value = "HHH-1689" ),
			@JiraKey( value = "SQM-30" )
	} )
	public void testSubQueryAsSimpleCaseWhenExpression() {
		final String query = "SELECT CASE l.id WHEN (SELECT COUNT(r.id) FROM Root r) THEN 1 ELSE 0 END FROM Leaf l";
		// simple syntax check
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( query ).list();
		s.getTransaction().commit();
		s.close();
	}
}
