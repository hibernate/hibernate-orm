/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.list;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.query.spi.ScrollableResultsImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/collection/list/ParentChildMapping.xml"
)
@SessionFactory
public class IterateOverListInTheSetMethodTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}


	@Test
	@JiraKey(value = "HHH-16120")
	public void testHqlQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Child child = new Child( 1, "Luigi" );
					Child child2 = new Child( 2, "Franco" );
					Parent parent = new Parent( 2, "Fabio" );
					parent.addChild( child );
					parent.addChild( child2 );

					session.persist( parent );
					session.persist( child );
					session.persist( child2 );
				}
		);
		scope.inTransaction(
				session -> {
					session.createQuery( "select p from Parent p", Parent.class ).list();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-16184")
	public void testSelectParentsWithoutChildren(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( 2, "Fabio" );
					session.persist( parent );
				}
		);

		SQLStatementInspector collectingStatementInspector = scope.getCollectingStatementInspector();
		collectingStatementInspector.clear();
		scope.inTransaction(
				session -> {
					session.createQuery( "select p from Parent p", Parent.class ).list();

				}
		);
		assertThat( collectingStatementInspector.getSqlQueries().size() ).isEqualTo( 2 );
	}

	@Test
	@JiraKey(value = "HHH-16184")
	public void testScrollParentsWithoutChildren(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( 2, "Fabio" );
					session.persist( parent );
				}
		);

		SQLStatementInspector collectingStatementInspector = scope.getCollectingStatementInspector();
		collectingStatementInspector.clear();
		scope.inTransaction(
				session -> {
					try (ScrollableResultsImplementor<Parent> results = session.createQuery(
									"select p from Parent p",
									Parent.class
							)
							.scroll()) {
						List<Parent> list = new ArrayList<>();
						while ( results.next() ) {
							list.add( results.get() );
						}
						assertThat( list.size() ).isEqualTo( 1 );
					}

				}
		);
		assertThat( collectingStatementInspector.getSqlQueries().size() ).isEqualTo( 2 );
	}

	@Test
	@JiraKey(value = "HHH-16184")
	public void testSelectParentsWithoutChildren2(SessionFactoryScope scope) {
		Integer parentId = 2;
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( parentId, "Fabio" );
					session.persist( parent );
				}
		);

		SQLStatementInspector collectingStatementInspector = scope.getCollectingStatementInspector();
		collectingStatementInspector.clear();
		scope.inTransaction(
				session -> {
					session.createQuery( "select p from Parent p where p.id = :id", Parent.class )
							.setParameter( "id", parentId )
							.uniqueResult();

				}
		);
		assertThat( collectingStatementInspector.getSqlQueries().size() ).isEqualTo( 2 );
	}

	@Test
	@JiraKey(value = "HHH-16184")
	public void testSelectParentsWithChildren(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Child child = new Child( 1, "Luigi" );
					Child child2 = new Child( 2, "Franco" );
					Parent parent = new Parent( 2, "Fabio" );
					parent.addChild( child );
					parent.addChild( child2 );

					session.persist( parent );
					session.persist( child );
					session.persist( child2 );
				}
		);

		SQLStatementInspector collectingStatementInspector = scope.getCollectingStatementInspector();
		collectingStatementInspector.clear();
		scope.inTransaction(
				session -> {
					session.createQuery( "select p from Parent p", Parent.class ).list();

				}
		);
		assertThat( collectingStatementInspector.getSqlQueries().size() ).isEqualTo( 2 );
	}
}
