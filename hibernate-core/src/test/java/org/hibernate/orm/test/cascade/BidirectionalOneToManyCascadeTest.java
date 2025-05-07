/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test case to illustrate that when a delete-orphan cascade is used on a
 * one-to-many collection and the many-to-one side is also cascaded a
 * TransientObjectException is thrown.
 * <p>
 * (based on annotations test case submitted by Edward Costello)
 *
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/cascade/Child.xml",
				"org/hibernate/orm/test/cascade/DeleteOrphanChild.xml",
				"org/hibernate/orm/test/cascade/Parent.xml"
		}
)
@SessionFactory
public class BidirectionalOneToManyCascadeTest {

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	/**
	 * Saves the parent object with a child when both the one-to-many and
	 * many-to-one associations use cascade="all"
	 */
	@Test
	public void testSaveParentWithChild(SessionFactoryScope scope) {
		Parent parent = new Parent();
		scope.inTransaction(
				session -> {
					Child child = new Child();
					child.setParent( parent );
					parent.setChildren( Collections.singleton( child ) );
					session.persist( parent );
				}
		);

		scope.inTransaction(
				session -> {
					Parent result = session.find( Parent.class, parent.getId() );
					assertEquals( 1, result.getChildren().size() );
					assertEquals( 0, result.getDeleteOrphanChildren().size() );
				}
		);
	}

	/**
	 * Saves the child object with the parent when both the one-to-many and
	 * many-to-one associations use cascade="all"
	 */
	@Test
	public void testSaveChildWithParent(SessionFactoryScope scope) {

		Parent parent = new Parent();
		scope.inTransaction(
				session -> {
					Child child = new Child();
					child.setParent( parent );
					parent.setChildren( Collections.singleton( child ) );
					session.persist( child );
				}
		);

		scope.inTransaction(
				session -> {
					Parent result = session.find( Parent.class, parent.getId() );
					assertEquals( 1, result.getChildren().size() );
					assertEquals( 0, result.getDeleteOrphanChildren().size() );
				}
		);
	}

	/**
	 * Saves the parent object with a child when the one-to-many association
	 * uses cascade="all-delete-orphan" and the many-to-one association uses
	 * cascade="all"
	 */
	@Test
	public void testSaveParentWithOrphanDeleteChild(SessionFactoryScope scope) {
		Parent parent = new Parent();

		scope.inTransaction(
				session -> {
					DeleteOrphanChild child = new DeleteOrphanChild();
					child.setParent( parent );
					parent.setDeleteOrphanChildren( Collections.singleton( child ) );
					session.persist( parent );
				}
		);

		scope.inTransaction(
				session -> {
					Parent result = session.find( Parent.class, parent.getId() );
					assertEquals( 0, result.getChildren().size() );
					assertEquals( 1, result.getDeleteOrphanChildren().size() );
				}
		);
	}

	/**
	 * Saves the child object with the parent when the one-to-many association
	 * uses cascade="all-delete-orphan" and the many-to-one association uses
	 * cascade="all"
	 */
	@Test
	public void testSaveOrphanDeleteChildWithParent(SessionFactoryScope scope) {
		Parent parent = new Parent();
		scope.inTransaction(
				session -> {
					DeleteOrphanChild child = new DeleteOrphanChild();
					child.setParent( parent );
					parent.setDeleteOrphanChildren( Collections.singleton( child ) );
					session.persist( child );
				}
		);

		scope.inTransaction(
				session -> {
					Parent result = session.find( Parent.class, parent.getId() );
					assertEquals( 0, result.getChildren().size() );
					assertEquals( 1, result.getDeleteOrphanChildren().size() );
				}
		);
	}
}
