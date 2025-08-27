/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ops;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Gavin King
 * @author Hardy Ferentschik
 */
@Jpa(
		integrationSettings = {
				@Setting(name = Environment.GENERATE_STATISTICS, value = "true"),
				@Setting(name = Environment.STATEMENT_BATCH_SIZE, value = "0")
		},
		xmlMappings = {
				"org/hibernate/orm/test/jpa/ops/Node.hbm.xml"
		}
)
public class MergeTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testMergeTree(EntityManagerFactoryScope scope) {
		clearCounts( scope );

		Node root = new Node( "root" );
		Node child = new Node( "child" );

		scope.inTransaction(
				entityManager -> {
					root.addChild( child );
					entityManager.persist( root );
				}
		);

		assertInsertCount( scope, 2 );
		clearCounts( scope );

		root.setDescription( "The root node" );
		child.setDescription( "The child node" );

		Node secondChild = new Node( "second child" );

		root.addChild( secondChild );

		scope.inTransaction(
				entityManager ->
						entityManager.merge( root )
		);

		assertInsertCount( scope, 1 );
		assertUpdateCount( scope, 2 );
	}

	@Test
	public void testMergeTreeWithGeneratedId(EntityManagerFactoryScope scope) {
		clearCounts( scope );

		NumberedNode root = new NumberedNode( "root" );
		NumberedNode child = new NumberedNode( "child" );
		root.addChild( child );

		scope.inTransaction(
				entityManager ->
						entityManager.persist( root )
		);

		assertInsertCount( scope, 2 );
		clearCounts( scope );

		root.setDescription( "The root node" );
		child.setDescription( "The child node" );

		NumberedNode secondChild = new NumberedNode( "second child" );

		root.addChild( secondChild );

		scope.inTransaction(
				entityManager ->
						entityManager.merge( root )
		);

		assertInsertCount( scope, 1 );
		assertUpdateCount( scope, 2 );
	}

	private void clearCounts(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class ).getStatistics().clear();
	}

	private void assertInsertCount(EntityManagerFactoryScope scope, int count) {
		int inserts = (int) scope.getEntityManagerFactory()
				.unwrap( SessionFactoryImplementor.class )
				.getStatistics()
				.getEntityInsertCount();
		Assertions.assertEquals( count, inserts );
	}

	private void assertUpdateCount(EntityManagerFactoryScope scope, int count) {
		int updates = (int) scope.getEntityManagerFactory()
				.unwrap( SessionFactoryImplementor.class )
				.getStatistics()
				.getEntityUpdateCount();
		Assertions.assertEquals( count, updates );
	}
}
