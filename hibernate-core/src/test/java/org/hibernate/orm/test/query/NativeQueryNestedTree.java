/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import jakarta.persistence.*;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * This reproduces an issue in Hibernate 6 parsing native queries.
 */
@DomainModel(
	annotatedClasses = {
		NativeQueryNestedTree.Tree.class,
		NativeQueryNestedTree.Forest.class
	}
)
@ServiceRegistry(
	settings = {
		@Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
		@Setting(name = AvailableSettings.HBM2DDL_AUTO, value = "create")
	}
)
@SessionFactory
@JiraKey(value = "HHH-18871")
public class NativeQueryNestedTree {

	@Test
	public void test(SessionFactoryScope scope) {
		// We want to make sure 'Could not locate TableGroup' no longer is thrown
		assertDoesNotThrow( () -> scope.inTransaction( session ->
			session.createNativeQuery( """
				SELECT {t.*}, {t2.*}, {t3.*}
				FROM TREE t
				INNER JOIN tree t2 ON t2.parent_id = t.id
				INNER JOIN tree t3 ON t3.parent_id = t2.id
			""" )
				.addEntity( "t", Tree.class )
				.addJoin( "t2", "t.children" )
				.addJoin( "t3", "t2.children" )
				.list()
		) );

		assertDoesNotThrow( () -> scope.inTransaction( session ->
			session.createNativeQuery("""
				SELECT {t.*}, {t2.*}, {t3.*}, {t4.*}
				FROM tree t
				INNER JOIN tree t2 ON t2.parent_id = t.id
				INNER JOIN tree t3 ON t3.parent_id = t2.id
				INNER JOIN tree t4 ON t4.parent_id = t3.id
			""")
				.addEntity("t", Tree.class)
				.addJoin("t2", "t.children")
				.addJoin("t3", "t2.children")
				.addJoin("t4", "t3.children")
				.list()
		) );

		assertDoesNotThrow( () -> scope.inTransaction( session ->
			session.createNativeQuery("""
				SELECT {f.*}, {t.*}, {t2.*}
				FROM forest f
				INNER JOIN tree t ON t.parent_id IS NULL
				INNER JOIN tree t2 ON t2.parent_id = t.id
			""")
				.addEntity("f", Forest.class)
				.addJoin("t", "f.trees")
				.addJoin("t2", "t.children")
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
