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
