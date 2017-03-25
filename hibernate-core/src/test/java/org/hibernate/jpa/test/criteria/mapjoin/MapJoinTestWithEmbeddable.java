/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.mapjoin;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Christian Beikov
 */
public class MapJoinTestWithEmbeddable extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Batch.class, Node.class, BatchNodeMetadata.class};
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10455" )
	public void testSelectingKeyOfMapJoin() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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
	@TestForIssue( jiraKey = "HHH-10229" )
	public void testSelectingValueOfMapJoin() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Node> query = cb.createQuery( Node.class );
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
		private NodeMigration migrering = NodeMigration.TOTAL;

		public NodeMigration getMigrering() {
			return migrering;
		}

		public void setMigrering(NodeMigration migrering) {
			this.migrering = migrering;
		}
	}

	public static enum NodeMigration {
		TOTAL
	}
}
