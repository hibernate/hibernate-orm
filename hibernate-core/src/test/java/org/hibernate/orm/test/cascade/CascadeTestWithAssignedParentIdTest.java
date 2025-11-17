/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade;

import java.util.Collections;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test case to illustrate that when a child table attempts to cascade to a parent and the parent's Id
 * is set to assigned, an exception thrown (not-null property references a null or transient value).
 * This error only occurs if the parent link in marked as not nullable.
 *
 * @author Wallace Wadge (based on code by Gail Badner)
 */
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/cascade/ChildForParentWithAssignedId.hbm.xml",
				"org/hibernate/orm/test/cascade/ParentWithAssignedId.hbm.xml"
		}
)
@SessionFactory
public class CascadeTestWithAssignedParentIdTest {

	@Test
	public void testSaveChildWithParent(SessionFactoryScope scope) {
		Parent parent = new Parent();
		scope.inTransaction(
				session -> {
					Child child = new Child();
					child.setParent( parent );
					parent.setChildren( Collections.singleton( child ) );
					parent.setId( Long.valueOf( 123L ) );
					// this should figure out that the parent needs saving first since id is assigned.
					session.persist( child );
				}
		);

		scope.inTransaction(
				session -> {
					Parent result = session.get( Parent.class, parent.getId() );
					assertEquals( 1, result.getChildren().size() );
				}
		);
	}
}
