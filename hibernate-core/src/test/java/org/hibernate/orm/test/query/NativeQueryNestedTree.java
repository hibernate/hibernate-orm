/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import jakarta.persistence.*;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * This reproduces an issue in Hibernate 6 parsing native queries.
 */
@DomainModel(
		annotatedClasses = {
				NativeQueryNestedTree.TreeNode.class
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

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		// fill it with data
		scope.inTransaction(
				session -> {
					var root = new TreeNode("root", null, null);
					session.persist(root);

					var level1_1 = new TreeNode("level1_1", root, null);
					session.persist(level1_1);
					var level1_2 = new TreeNode("level1_2", root, null);
					session.persist(level1_2);

					var level1_1_1 = new TreeNode("level1_1_1", level1_1, null);
					session.persist(level1_1_1);
					var level1_1_2 = new TreeNode("level1_1_2", level1_1, null);
					session.persist(level1_1_2);

					var level1_2_1 = new TreeNode("level1_2_1", level1_2, null);
					session.persist(level1_2_1);
					var level1_2_2 = new TreeNode("level1_2_2", level1_2, null);
					session.persist(level1_2_2);

					var level1_1_1_1 = new TreeNode("level1_1_1_1", level1_1, null);
					session.persist(level1_1_1_1);
					var level1_1_1_2 = new TreeNode("level1_1_1_2", level1_1, null);
					session.persist(level1_1_1_2);
				});
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery("delete from TreeNode").executeUpdate();;
				}
		);
	}

	@Test
	public void test3Levels(SessionFactoryScope scope) {

		// This query will throw a Could not locate TableGroup exception
		scope.inTransaction(
				session -> {
					var query = session.createNativeQuery(
									"""
											SELECT {t.*}, {t2.*}, {t3.*}
											FROM TREE t
											INNER JOIN tree t2 ON t2.parentident = t.ident
											INNER JOIN tree t3 ON t3.parentident = t2.ident
											WHERE t.name = 'root'
											""")
							.addEntity("t", TreeNode.class)
							.addJoin("t2", "t.children")
							.addJoin("t3", "t2.children")
							.setTimeout(10);
					var item = query.list();

					System.out.println("Retrieved SQL items: " + item);
				});
	}

	@Test
	public void test2Levels(SessionFactoryScope scope) {

		// This query works
		scope.inTransaction(
				session -> {
					var query = session.createNativeQuery(
									"""
											SELECT {t.*}, {t2.*}
											FROM TREE t
											INNER JOIN tree t2 ON t2.parentident = t.ident
											WHERE t.name = 'root'
											""")
							.addEntity("t", TreeNode.class)
							.addJoin("t2", "t.children")
							.setTimeout(10);
					var item = query.list();

					System.out.println("Retrieved SQL items: " + item);
				});
	}

	@Entity(name = "TreeNode")
	@Table(name = "TREE")
	public static class TreeNode {
		private String name;

		@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@JoinColumn(name = "parentident")
		private TreeNode parent;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		private Set<TreeNode> children = new HashSet<TreeNode>();

		@Id
		@GeneratedValue
		private long ident;

		public TreeNode() {
		}

		public TreeNode(String name,
						TreeNode parent,
						Set<TreeNode> children) {
			this.name = name;
			this.parent = parent;
			this.children = children;
		}

		public String getName() {
			return name;
		}

		public TreeNode getParent() {
			return parent;
		}

		public Set<TreeNode> getChildren() {
			return children;
		}

		@Override
		public String toString() {
			return "TreeNode{" +
					"value='" + name + '\'' +
					", ident=" + ident +
					'}';
		}
	}
}
