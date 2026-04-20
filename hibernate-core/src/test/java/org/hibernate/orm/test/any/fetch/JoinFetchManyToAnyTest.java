/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.fetch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import org.hibernate.Hibernate;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory(useCollectingStatementInspector = true)
@DomainModel(annotatedClasses = {JoinFetchManyToAnyTest.Parent.class,
		JoinFetchManyToAnyTest.NormalChild.class, JoinFetchManyToAnyTest.SpecialChild.class})
class JoinFetchManyToAnyTest {
	@Entity(name = "Parent")
	static class Parent {
		@Id
		@GeneratedValue
		Long id;

		@ManyToAny(fetch = FetchType.LAZY)
		@AnyKeyJavaClass(Long.class)
		@Column(name = "child_type")
		@AnyDiscriminatorValue(
				discriminator = "Normal",
				entity = NormalChild.class)
		@AnyDiscriminatorValue(
				discriminator = "Special",
				entity = SpecialChild.class)
		@JoinTable(name = "parent_child",
				joinColumns = @JoinColumn(name = "child_id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id"))
		Set<Child> children;
	}
	interface Child {}
	@Entity(name = "NormalChild")
	static class NormalChild implements Child {
		@Id
		@GeneratedValue
		Long id;
		int intValue;
	}
	@Entity(name = "SpecialChild")
	static class SpecialChild implements Child {
		@Id
		@GeneratedValue
		Long id;
		String strValue;
	}

	@Test
	void test(SessionFactoryScope scope) {
		final var statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(s -> {
			Parent parent = new Parent();
			parent.children = Set.of(new NormalChild(), new SpecialChild());
			s.persist(parent);
			parent.children.forEach(s::persist);
		});
		statementInspector.clear();
		scope.inTransaction(s -> {
			Parent parent = s.createQuery("from Parent", Parent.class).getSingleResult();
			assertFalse( Hibernate.isInitialized( parent.children ) );
			assertEquals( 1, statementInspector.getSqlQueries().size() );
			assertEquals( 2, parent.children.size() );
			assertTrue( Hibernate.isInitialized( parent.children ) );
			assertEquals( 2, statementInspector.getSqlQueries().size() );
		});
		statementInspector.clear();
		scope.inTransaction(s -> {
			Parent parent = s.createQuery("from Parent left join fetch children", Parent.class).getSingleResult();
			assertTrue( Hibernate.isInitialized( parent.children ) );
			assertEquals( 1, statementInspector.getSqlQueries().size() );
			assertEquals( 2, parent.children.size() );
			assertEquals( 1, statementInspector.getSqlQueries().size() );
		});
		statementInspector.clear();
		scope.inTransaction(s -> {
			Parent parent = s.find(Parent.class, 1L);
			assertFalse( Hibernate.isInitialized( parent.children ) );
			assertEquals( 1, statementInspector.getSqlQueries().size() );
			assertEquals( 2, parent.children.size() );
			assertTrue( Hibernate.isInitialized( parent.children ) );
			assertEquals( 2, statementInspector.getSqlQueries().size() );
		});
		statementInspector.clear();
		scope.inTransaction(s -> {
			var graph = s.createEntityGraph(Parent.class);
			graph.addAttributeNode( JoinFetchManyToAnyTest_.Parent_.children );
			Parent parent = s.find(graph, 1L);
			assertTrue( Hibernate.isInitialized( parent.children ) );
			assertEquals( 1, statementInspector.getSqlQueries().size() );
			assertEquals( 2, parent.children.size() );
			assertEquals( 1, statementInspector.getSqlQueries().size() );
		});
	}
}
