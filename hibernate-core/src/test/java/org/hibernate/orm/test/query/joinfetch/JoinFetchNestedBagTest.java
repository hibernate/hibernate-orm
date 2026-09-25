/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.joinfetch;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = {
				JoinFetchNestedBagTest.Node.class,
				JoinFetchNestedBagTest.NodeHolder.class
		}
)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-10624")
public class JoinFetchNestedBagTest {

	@Test
	public void testSingular(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Node leafNode = new Node( "leaf", Collections.emptyList() );
			session.persist( leafNode );
			Node parentNode = new Node( "parent", Collections.singletonList( leafNode ) );
			session.persist( parentNode );
			session.persist( new NodeHolder( parentNode ) );
			session.persist( new NodeHolder( parentNode ) );
		} );
		scope.inTransaction( session -> {
			final List<NodeHolder> holders = session.createQuery(
					"from NodeHolder h join fetch h.node d join fetch d.children",
					NodeHolder.class
			).getResultList();
			assertEquals( 2, holders.size() );
			assertEquals( 1, holders.get( 0 ).getNode().getChildren().size() );
		} );
	}

	@Test
	public void testSet(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Node leafNode = new Node( "leaf", Collections.emptyList() );
			session.persist( leafNode );
			Node parentNode = new Node( "parent", Collections.singletonList( leafNode ) );
			session.persist( parentNode );
			session.persist( new NodeHolder( parentNode ) );
			session.persist( new NodeHolder( parentNode ) );
		} );
		scope.inTransaction( session -> {
			final List<NodeHolder> holders = session.createQuery(
					"from NodeHolder h join fetch h.nodeSet d join fetch d.children",
					NodeHolder.class
			).getResultList();
			assertEquals( 2, holders.size() );
			assertEquals( 1, holders.get( 0 ).getNodeSet().size() );
			assertEquals( 1, holders.get( 0 ).getNodeSet().iterator().next().getChildren().size() );
		} );
	}

	@Test
	public void testList(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Node leafNode = new Node( "leaf", Collections.emptyList() );
			session.persist( leafNode );
			Node parentNode = new Node( "parent", Collections.singletonList( leafNode ) );
			session.persist( parentNode );
			NodeHolder nodeHolder = new NodeHolder();
			nodeHolder.getNodeList().add( parentNode );
			nodeHolder.getNodeList().add( parentNode );
			session.persist( nodeHolder );
		} );
		scope.inTransaction( session -> {
			final List<NodeHolder> holders = session.createQuery(
					"from NodeHolder h join fetch h.nodeList d join fetch d.children",
					NodeHolder.class
			).getResultList();
			assertEquals( 1, holders.size() );
			assertEquals( 2, holders.get( 0 ).getNodeList().size() );
			assertEquals( 1, holders.get( 0 ).getNodeList().get( 0 ).getChildren().size() );
		} );
	}

	@Test
	public void testMap(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Node leafNode = new Node( "leaf", Collections.emptyList() );
			session.persist( leafNode );
			Node parentNode = new Node( "parent", Collections.singletonList( leafNode ) );
			session.persist( parentNode );
			NodeHolder nodeHolder = new NodeHolder();
			nodeHolder.getNodeMap().put( "a", parentNode );
			nodeHolder.getNodeMap().put( "b", parentNode );
			session.persist( nodeHolder );
		} );
		scope.inTransaction( session -> {
			final List<NodeHolder> holders = session.createQuery(
					"from NodeHolder h join fetch h.nodeMap d join fetch d.children",
					NodeHolder.class
			).getResultList();
			assertEquals( 1, holders.size() );
			assertEquals( 2, holders.get( 0 ).getNodeMap().size() );
			assertEquals( 1, holders.get( 0 ).getNodeMap().get( "a" ).getChildren().size() );
		} );
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "Node")
	public static class Node {
		@Id
		private String name;
		@OneToMany
		private List<Node> children;

		public Node() {
		}

		public Node(String name, List<Node> children) {
			this.name = name;
			this.children = new ArrayList<>( children );
		}

		public String getName() {
			return name;
		}

		public List<Node> getChildren() {
			return children;
		}
	}

	@Entity(name = "NodeHolder")
	public static class NodeHolder {
		@Id
		@GeneratedValue
		private Long id;
		@ManyToOne
		private Node node;
		@ManyToMany
		@OrderColumn
		@JoinTable(name = "node_list")
		private List<Node> nodeList = new ArrayList<>();
		@ManyToMany
		@JoinTable(name = "node_map")
		private Map<String, Node> nodeMap = new HashMap<>();
		@ManyToMany
		@JoinTable(name = "node_set")
		private Set<Node> nodeSet = new HashSet<>();

		public NodeHolder() {
		}

		public NodeHolder(Node node) {
			this.node = node;
			this.nodeSet.add( node );
		}

		public Node getNode() {
			return node;
		}

		public List<Node> getNodeList() {
			return nodeList;
		}

		public Map<String, Node> getNodeMap() {
			return nodeMap;
		}

		public Set<Node> getNodeSet() {
			return nodeSet;
		}
	}
}
