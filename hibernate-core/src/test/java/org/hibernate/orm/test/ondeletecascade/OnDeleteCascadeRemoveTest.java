/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ondeletecascade;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(annotatedClasses =
		{OnDeleteCascadeRemoveTest.Parent.class,
		OnDeleteCascadeRemoveTest.Child.class},
		generateStatistics = true,
		useCollectingStatementInspector = true)
//@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class)
class OnDeleteCascadeRemoveTest {
	@Test
	void testOnDeleteCascadeRemove1(EntityManagerFactoryScope scope) {
		Statistics statistics =
				scope.getEntityManagerFactory().unwrap( SessionFactory.class )
						.getStatistics();
		statistics.clear();
		var inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( em -> {
			Parent parent = new Parent();
			Child child = new Child();
			parent.children.add( child );
			child.parent = parent;
			em.persist( parent );
		} );
		scope.inTransaction( em -> {
			Parent parent = em.find( Parent.class, 0L );
			assertFalse( Hibernate.isInitialized( parent.children ) );
			em.remove( parent );
			// note: ideally we would skip the initialization here
			assertTrue( Hibernate.isInitialized( parent.children ) );
		});
		EntityStatistics entityStatistics = statistics.getEntityStatistics( Child.class.getName() );
		assertEquals( 1L, entityStatistics.getDeleteCount() );
		assertEquals( 1L, entityStatistics.getCacheRemoveCount() );
		inspector.assertExecutedCount( scope.getDialect().supportsCascadeDelete() ? 5 : 6 );
		long children =
				scope.fromTransaction( em -> em.createQuery( "select count(*) from CascadeChild", Long.class )
						.getSingleResult() );
		assertEquals( 0L, children );
	}

	@Test
	void testOnDeleteCascadeRemove2(EntityManagerFactoryScope scope) {
		Statistics statistics =
				scope.getEntityManagerFactory().unwrap( SessionFactory.class )
						.getStatistics();
		statistics.clear();
		var inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( em -> {
			Parent parent = new Parent();
			Child child = new Child();
			parent.children.add( child );
			child.parent = parent;
			em.persist( parent );
		} );
		scope.inTransaction( em -> {
			Parent parent = em.find( Parent.class, 0L );
			assertEquals(1, parent.children.size());
			assertTrue( Hibernate.isInitialized( parent.children ) );
			em.remove( parent );
			assertTrue( em.unwrap( SessionImplementor.class )
					.getPersistenceContext()
					.getEntry( parent.children.iterator().next() )
					.getStatus().isDeletedOrGone() );
		});
		EntityStatistics entityStatistics = statistics.getEntityStatistics( Child.class.getName() );
		assertEquals( 1L, entityStatistics.getDeleteCount() );
		assertEquals( 1L, entityStatistics.getCacheRemoveCount() );
		inspector.assertExecutedCount( scope.getDialect().supportsCascadeDelete() ? 5 : 6 );
		long children =
				scope.fromTransaction( em -> em.createQuery( "select count(*) from CascadeChild", Long.class )
						.getSingleResult() );
		assertEquals( 0L, children );
	}

	@Entity(name="CascadeParent")
	static class Parent {
		@Id
		long id;
		@OneToMany(mappedBy = "parent",
				cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
		@OnDelete(action = OnDeleteAction.CASCADE)
		Set<Child> children = new HashSet<>();
	}
	@Entity(name="CascadeChild")
	@Cacheable
//	@SQLDelete( sql = "should never happen" )
	static class Child {
		@Id
		long id;
		@OnDelete(action = OnDeleteAction.CASCADE)
		@ManyToOne
		Parent parent;
	}
}
