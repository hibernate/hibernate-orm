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

	@Test
	public void test(SessionFactoryScope scope) {
		// We want to make sure 'Could not locate TableGroup' no longer is thrown
		assertDoesNotThrow( () ->
			scope.inTransaction( session ->
				session.createNativeQuery( """
					SELECT {t.*}, {t2.*}, {t3.*}
					FROM TREE t
					INNER JOIN tree t2 ON t2.parentident = t.ident
					INNER JOIN tree t3 ON t3.parentident = t2.ident
				""" )
					.addEntity( "t", TreeNode.class )
					.addJoin( "t2", "t.children" )
					.addJoin( "t3", "t2.children" )
					.list()
			)
		);

		assertDoesNotThrow(() ->
			scope.inTransaction(session ->
				session.createNativeQuery("""
						SELECT {t.*}, {t2.*}, {t3.*}, {t4.*}
						FROM tree t
						INNER JOIN tree t2 ON t2.parentident = t.ident
						INNER JOIN tree t3 ON t3.parentident = t2.ident
						INNER JOIN tree t4 ON t4.parentident = t3.ident
					""")
					.addEntity("t", TreeNode.class)
					.addJoin("t2", "t.children")
					.addJoin("t3", "t2.children")
					.addJoin("t4", "t3.children")
					.list()
			)
		);

		// Let's get crazy
		assertDoesNotThrow(() ->
			scope.inTransaction(session ->
				session.createNativeQuery("""
						SELECT {t.*}, {t2.*}, {t3.*}, {t4.*}, {t5.*}
						FROM tree t
						INNER JOIN tree t2 ON t2.parentident = t.ident
						INNER JOIN tree t3 ON t3.parentident = t2.ident
						INNER JOIN tree t4 ON t4.parentident = t3.ident
						INNER JOIN tree t5 ON t5.parentident = t4.ident
					""")
					.addEntity("t", TreeNode.class)
					.addJoin("t2", "t.children")
					.addJoin("t3", "t2.children")
					.addJoin("t4", "t3.children")
					.addJoin("t5", "t4.children")
					.list()
			)
		);
	}

	@Entity(name = "TreeNode")
	@Table(name = "TREE")
	public static class TreeNode {
		@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@JoinColumn(name = "parentident")
		private TreeNode parent;
		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		private Set<TreeNode> children = new HashSet<TreeNode>();
		@Id
		@GeneratedValue
		private long ident;
	}
}
