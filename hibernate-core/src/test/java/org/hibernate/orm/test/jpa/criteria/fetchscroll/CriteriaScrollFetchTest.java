/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.fetchscroll;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.ScrollableResults;
import org.hibernate.orm.test.hqlfetchscroll.Child;
import org.hibernate.orm.test.hqlfetchscroll.Parent;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		xmlMappings = "org/hibernate/orm/test/hqlfetchscroll/ParentChild.hbm.xml"
)
@SessionFactory
public class CriteriaScrollFetchTest {

	@Test
	public final void testListWithFetch(SessionFactoryScope scope) {
		scope.inSession(
				s -> {
					CriteriaBuilder cb = s.getCriteriaBuilder();
					CriteriaQuery<Parent> cq = cb.createQuery(Parent.class);
					Root<Parent> from = cq.distinct( true ).from( Parent.class );
					CriteriaQuery<Parent> select = cq.select(from);
					from.fetch( "children", JoinType.LEFT );
					List<Parent> l = s.createQuery( select ).list();
					assertResultFromAllParents(l);
				}
		);
	}

	@Test
	public final void testListWithoutFetch(SessionFactoryScope scope) {
		scope.inSession(
				s -> {
					CriteriaBuilder cb = s.getCriteriaBuilder();
					CriteriaQuery<Parent> cq = cb.createQuery(Parent.class);
					Root<Parent> from = cq.distinct( true ).from( Parent.class );
					CriteriaQuery<Parent> select = cq.select(from);
					List<Parent> l = s.createQuery( select ).list();
					assertResultFromAllParents(l);
				}
		);
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-17497")
	public final void testScrollWithFetch(SessionFactoryScope scope) {
		scope.inSession(
				s -> {
					CriteriaBuilder cb = s.getCriteriaBuilder();
					CriteriaQuery<Parent> cq = cb.createQuery(Parent.class);
					Root<Parent> from = cq.distinct( true ).from( Parent.class );
					CriteriaQuery<Parent> select = cq.select(from);
					Fetch<Parent, Child> c = from.fetch( "children", JoinType.LEFT );
					cq.orderBy( cb.asc(from.get("id")), cb.asc(((Path<?>) c).get("id")) );
					ScrollableResults<Parent> sr = s.createQuery( select ).scroll();
					assertResultFromAllParents(makeList(sr));
				}
		);
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-17497")
	public final void testScrollWithoutFetch(SessionFactoryScope scope) {
		scope.inSession(
				s -> {
					CriteriaBuilder cb = s.getCriteriaBuilder();
					CriteriaQuery<Parent> cq = s.getCriteriaBuilder().createQuery(Parent.class);
					Root<Parent> from = cq.distinct( true ).from( Parent.class );
					CriteriaQuery<Parent> select = cq.select(from);
					cq.orderBy( cb.asc(from.get("id")) );
					ScrollableResults<Parent> sr = s.createQuery( select ).scroll();
					assertResultFromAllParents(makeList(sr));
				}
		);
	}

	private List<Parent> makeList(ScrollableResults<Parent> results) {
		List<Parent> list = new ArrayList<>();
		while (results.next()) {
			list.add(results.get());
		}
		results.close();
		return list;
	}

	private void assertResultFromOneParent(Parent parent) {
		assertEquals(
				3,
				parent.getChildren().size(),
				"parent " + parent + " has incorrect collection(" + parent.getChildren() + ")."
		);
	}

	private void assertResultFromAllParents(List<Parent> list) {
		assertEquals( 2, list.size(), "list is not correct size: " );
		for ( Parent aList : list ) {
			assertResultFromOneParent( aList );
		}
	}

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Child child_1_1 = new Child( "achild1-1" );
					Child child_1_2 = new Child( "ychild1-2" );
					Child child_1_3 = new Child( "dchild1-3" );
					Child child_2_1 = new Child( "bchild2-1" );
					Child child_2_2 = new Child( "cchild2-2" );
					Child child_2_3 = new Child( "zchild2-3" );

					session.persist( child_1_1 );
					session.persist( child_2_1 );
					session.persist( child_1_2 );
					session.persist( child_2_2 );
					session.persist( child_1_3 );
					session.persist( child_2_3 );

					session.flush();

					Parent p1 = new Parent( "parent1" );
					p1.addChild( child_1_1 );
					p1.addChild( child_1_2 );
					p1.addChild( child_1_3 );
					session.persist( p1 );

					Parent p2 = new Parent( "parent2" );
					p2.addChild( child_2_1 );
					p2.addChild( child_2_2 );
					p2.addChild( child_2_3 );
					session.persist( p2 );
				}
		);
	}

	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

}
