/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * This reproduces an issue in Hibernate 6 parsing native queries.
 */
@DomainModel(
		annotatedClasses = {
				NativeQueryNestedTreeTest.Tree.class,
				NativeQueryNestedTreeTest.Forest.class
		}
)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-18871")
public class NativeQueryNestedTreeTest {

	@Test
	public void test(SessionFactoryScope scope) {
		// We want to make sure 'Could not locate TableGroup' no longer is thrown
		assertDoesNotThrow( () -> scope.inTransaction( session ->
				session.createNativeQuery( """
								select {t.*}, {t2.*}, {t3.*}
								from tree t
								inner join tree t2 on t2.parent_id = t.id
								inner join tree t3 on t3.parent_id = t2.id
								""", Tree.class )
						.addEntity( "t", Tree.class )
						.addJoin( "t2", "t.children" )
						.addJoin( "t3", "t2.children" )
						.list()
		) );

		assertDoesNotThrow( () -> scope.inTransaction( session ->
				session.createNativeQuery( """
								select {t.*}, {t2.*}, {t3.*}, {t4.*}
								from tree t
								inner join tree t2 on t2.parent_id = t.id
								inner join tree t3 on t3.parent_id = t2.id
								inner join tree t4 on t4.parent_id = t3.id
								""", Tree.class )
						.addEntity( "t", Tree.class )
						.addJoin( "t2", "t.children" )
						.addJoin( "t3", "t2.children" )
						.addJoin( "t4", "t3.children" )
						.list()
		) );

		assertDoesNotThrow( () -> scope.inTransaction( session ->
				session.createNativeQuery( """
								select {f.*}, {t.*}, {t2.*}
								from forest f
								inner join tree t on t.parent_id is null
								inner join tree t2 on t2.parent_id = t.id
								""", Forest.class )
						.addEntity( "f", Forest.class )
						.addJoin( "t", "f.trees" )
						.addJoin( "t2", "t.children" )
						.list()
		) );
	}

	@Entity(name = "Tree")
	@Table(name = "tree")
	public static class Tree {
		@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@JoinColumn(name = "parent_id")
		private Tree parent;
		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		private Set<Tree> children = new HashSet<>();
		@Id
		@GeneratedValue
		private long id;
	}

	@Entity(name = "Forest")
	@Table(name = "forest")
	public static class Forest {
		@Id
		@GeneratedValue
		private Long id;
		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@JoinColumn(name = "forest_id")
		private Set<Tree> trees = new HashSet<>();
	}
}
