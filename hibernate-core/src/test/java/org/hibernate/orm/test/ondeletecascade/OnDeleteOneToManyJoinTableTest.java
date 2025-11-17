/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ondeletecascade;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.RollbackException;
import org.hibernate.Hibernate;
import org.hibernate.TransientObjectException;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Jpa(annotatedClasses =
		{OnDeleteOneToManyJoinTableTest.Parent.class, OnDeleteOneToManyJoinTableTest.Child.class},
		useCollectingStatementInspector = true)
//@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class)
public class OnDeleteOneToManyJoinTableTest {
	@Test
	public void testOnDeleteParent(EntityManagerFactoryScope scope) {
		var inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		Parent parent = new Parent();
		Child child = new Child();
		parent.children.add( child );
		scope.inTransaction( em -> {
			em.persist( parent );
			em.persist( child );
		} );
		inspector.assertExecutedCount( 3 );
		inspector.clear();
		scope.inTransaction( em -> {
			Parent p = em.find( Parent.class, parent.id );
			inspector.assertExecutedCount( 1 );
			em.remove( p );
			assertFalse( Hibernate.isInitialized( p.children ) );
		} );
		inspector.assertExecutedCount( scope.getDialect().supportsCascadeDelete() ? 2 : 3 );
		scope.inTransaction( em -> {
			assertNotNull( em.find( Child.class, child.id ) );
		} );

		scope.inTransaction( em -> {
			assertEquals( 1,
					em.createNativeQuery( "select count(*) from Child", Integer.class )
							.getSingleResultOrNull() );
			assertEquals( 0,
					em.createNativeQuery( "select count(*) from Parent_Child", Integer.class )
							.getSingleResultOrNull() );
			assertEquals( 0,
					em.createNativeQuery( "select count(*) from Parent", Integer.class )
							.getSingleResultOrNull() );
		});	}

	@Test
	public void testOnDeleteChildrenFails(EntityManagerFactoryScope scope) {
		Parent parent = new Parent();
		Child child = new Child();
		parent.children.add( child );
		scope.inTransaction( em -> {
			em.persist( parent );
			em.persist( child );
		} );
		try {
			scope.inTransaction( em -> {
				Parent p = em.find( Parent.class, parent.id );
				for ( Child c : p.children ) {
					em.remove( c );
				}
			} );
			fail();
		}
		catch (RollbackException re) {
			assertTrue(re.getCause().getCause() instanceof TransientObjectException);
		}
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Parent")
	static class Parent {
		@Id
		long id;
		@OneToMany
		@JoinTable(joinColumns = @JoinColumn(name = "parent_id"))
		@OnDelete(action = OnDeleteAction.CASCADE)
		Set<Child> children = new HashSet<>();
	}

	@Entity(name = "Child")
	static class Child {
		@Id
		long id;
	}
}
