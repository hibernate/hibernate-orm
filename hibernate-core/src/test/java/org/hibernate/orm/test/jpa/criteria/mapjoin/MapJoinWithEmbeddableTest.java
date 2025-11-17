/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.mapjoin;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

/**
 * @author Christian Beikov
 */
@Jpa(annotatedClasses = {MapJoinWithEmbeddableTest.Batch.class, MapJoinWithEmbeddableTest.Node.class, MapJoinWithEmbeddableTest.BatchNodeMetadata.class})
public class MapJoinWithEmbeddableTest {

	@Test
	@JiraKey( value = "HHH-10455" )
	public void testSelectingKeyOfMapJoin(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Node> query = cb.createQuery( Node.class );
			Root<Batch> root = query.from( Batch.class );

			MapJoin nodes = (MapJoin) root.join( "batchNodeMetadata" );

			query.select( nodes.key() );
			query.where( cb.equal( root.get( "id" ), 1 ) );

			entityManager.createQuery( query ).getResultList();
		} );
	}

	@Test
	@JiraKey( value = "HHH-10229" )
	public void testSelectingValueOfMapJoin(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<BatchNodeMetadata> query = cb.createQuery( BatchNodeMetadata.class );
			Root<Batch> root = query.from( Batch.class );

			MapJoin nodes = (MapJoin) root.join( "batchNodeMetadata" );

			query.select( nodes );
			query.where( cb.equal( root.get( "id" ), 1 ) );

			entityManager.createQuery( query ).getResultList();
		} );
	}

	@Entity
	@Table(name = "batch")
	public static class Batch implements Serializable {
		@Id
		@GeneratedValue(generator = "BatchGen")
		@SequenceGenerator(name = "BatchGen", sequenceName = "seq_batch", allocationSize = 1)
		private Long id;

		@ElementCollection
		@CollectionTable(
				name = "batch_node",
				//uniqueConstraints = @UniqueConstraint(columnNames = {"batch_id", "node_id"}),
				//foreignKey = @ForeignKey,
				joinColumns = @JoinColumn(name = "batch_id", nullable = false))
		@MapKeyJoinColumn(name = "node_id")
		private Map<Node, BatchNodeMetadata> batchNodeMetadata = new HashMap<>();
	}

	@Entity
	@Table(name = "node")
	public static class Node implements Serializable {
		@Id
		@GeneratedValue(generator = "NodeGen")
		@SequenceGenerator(name = "NodeGen", sequenceName = "seq_node", allocationSize = 1)
		private Long id;
	}

	@Embeddable
	public static class BatchNodeMetadata implements Serializable {

		@Column(nullable = false)
		@Enumerated(EnumType.STRING)
		private NodeMigration migration = NodeMigration.TOTAL;

		public NodeMigration getMigration() {
			return migration;
		}

		public void setMigration(NodeMigration migration) {
			this.migration = migration;
		}
	}

	public enum NodeMigration {
		TOTAL
	}
}
