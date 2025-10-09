/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * NOTE : some subquery related tests still exist in other test classes in the suite.
 * This is a later attempt to create a more targeted set of subquery related tests.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = { SubQueryTest.Root.class, SubQueryTest.Branch.class, SubQueryTest.Leaf.class })
@SessionFactory
public class SubQueryTest {
	@AfterEach
	void dropTestData(SessionFactoryScope sessions) {
		sessions.dropData();
	}

	@Test
	@JiraKey( value = "HHH-9090" )
	public void testCorrelatedJoin(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
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
			session.persist( leaf1 );
			session.persist( leaf2 );
			session.persist( root.branch );
			session.persist( root );
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
			session.persist( otherLeaf1 );
			session.persist( otherLeaf3 );
			session.persist( otherRoot.branch );
			session.persist( otherRoot );
		} );

		sessions.inTransaction( (session) -> {
			var qry = """
					from Root as r
					where r.branch.branchName = 'branch'
					and exists( from r.branch.leaves as s where s.leafName = 'leaf1')
					""";
			Root rootQueried = session.createQuery( qry, Root.class ).uniqueResult();
			Assertions.assertEquals( "root name", rootQueried.rootName );
			Assertions.assertEquals( "branch", rootQueried.branch.branchName );
			Assertions.assertEquals( "leaf1", rootQueried.branch.leaves.get( 0 ).leafName );
			Assertions.assertEquals( "leaf2", rootQueried.branch.leaves.get( 1 ).leafName );
		} );

		sessions.inTransaction( (session) -> {
			var qry = """
					from Root as r
					where r.branch.branchName = 'branch'
					and exists( from r.branch.leaves as s where s.leafName = 'leaf3')
					""";
			Assertions.assertNull( session.createQuery( qry ).uniqueResult() );
		} );

		sessions.inTransaction( (session) -> {
			var qry = """
					from Root as r
					where exists( from r.branch.leaves as s where r.branch.branchName = 'branch'
						and s.leafName = 'leaf1')
					""";
			var rootQueried = session.createQuery( qry, Root.class ).uniqueResult();
			Assertions.assertEquals( "root name", rootQueried.rootName );
			Assertions.assertEquals( "branch", rootQueried.branch.branchName );
			Assertions.assertEquals( "leaf1", rootQueried.branch.leaves.get( 0 ).leafName );
			Assertions.assertEquals( "leaf2", rootQueried.branch.leaves.get( 1 ).leafName );
		} );

		sessions.inTransaction( (session) -> {
			var qry = """
					from Root as r
					where exists( from Root r1 where r1.branch.branchName = r.branch.branchName and r1.branch.branchName != 'other branch')
					""";
			var rootQueried = session.createQuery( qry, Root.class ).uniqueResult();
			Assertions.assertEquals( "root name", rootQueried.rootName );
			Assertions.assertEquals( "branch", rootQueried.branch.branchName );
			Assertions.assertEquals( "leaf1", rootQueried.branch.leaves.get( 0 ).leafName );
			Assertions.assertEquals( "leaf2", rootQueried.branch.leaves.get( 1 ).leafName );
		} );

	}

	@Test
	@JiraKey( value = "HHH-1689" )
	@JiraKey( value = "SQM-30" )
	public void testSubQueryAsSearchedCaseResultExpression(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final String query = """
				SELECT CASE
					WHEN l.id IS NOT NULL
						THEN (SELECT COUNT(r.id) FROM Root r)
					ELSE 0
				END
				FROM Leaf l
				""";
			// simple syntax check
			session.createQuery( query ).list();
		} );
	}

	@Test
	@JiraKey( value = "HHH-1689" )
	@JiraKey( value = "SQM-30" )
	public void testSubQueryAsSearchedCaseExpression(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final String query = """
				SELECT CASE
					WHEN (SELECT COUNT(r.id) FROM Root r) > 1 THEN 1
					ELSE 0
				END
				FROM Leaf l
				""";
			// simple syntax check
			session.createQuery( query ).list();
		} );
	}

	@Test
	@JiraKey( value = "HHH-1689" )
	@JiraKey( value = "SQM-30" )
	public void testSubQueryAsCaseElseResultExpression(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final String query = """
					SELECT CASE WHEN l.id > 1 THEN 1
						ELSE (SELECT COUNT(r.id) FROM Root r)
					END FROM Leaf l
					""";
			// simple syntax check
			session.createQuery( query ).list();
		} );
	}

	@Test
	@JiraKey( value = "HHH-1689" )
	@JiraKey( value = "SQM-30" )
	public void testSubQueryAsSimpleCaseTestExpression(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final String query = """
					SELECT CASE (SELECT COUNT(r.id) FROM Root r)
						WHEN 1 THEN 1
						ELSE 0
					END
					FROM Leaf l
					""";
			// simple syntax check
			session.createQuery( query ).list();
		} );
	}

	@Test
	@JiraKey( value = "HHH-1689" )
	@JiraKey( value = "SQM-30" )
	public void testSubQueryAsSimpleCaseWhenExpression(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final String query = """
					SELECT CASE l.id
						WHEN (SELECT COUNT(r.id) FROM Root r) THEN 1
						ELSE 0
					END
					FROM Leaf l
					""";
			// simple syntax check
			session.createQuery( query ).list();
		} );
	}


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

}
