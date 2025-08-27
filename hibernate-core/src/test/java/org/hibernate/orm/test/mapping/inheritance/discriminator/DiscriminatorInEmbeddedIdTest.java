/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.discriminator;

import java.io.Serializable;

import org.hibernate.annotations.DiscriminatorOptions;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel( annotatedClasses = {
		DiscriminatorInEmbeddedIdTest.Node.class,
		DiscriminatorInEmbeddedIdTest.NodeId.class,
		DiscriminatorInEmbeddedIdTest.NodeA.class,
		DiscriminatorInEmbeddedIdTest.NodeB.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16340" )
public class DiscriminatorInEmbeddedIdTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( session -> {
			final NodeA m1 = new NodeA( new NodeId( 1L, "NODEA" ), "node_a" );
			session.persist( m1 );
			final NodeB e1 = new NodeB( new NodeId( 2L, "NODEB" ), "node_b" );
			session.persist( e1 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Node" ).executeUpdate() );
	}

	@Test
	public void test(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"from NodeA",
					NodeA.class
			).getSingleResult().getName() ).isEqualTo( "node_a" );
			assertThat( session.createQuery(
					"from NodeB",
					NodeB.class
			).getSingleResult().getName() ).isEqualTo( "node_b" );
		} );
	}

	@Entity( name = "Node" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( name = "type" )
	@DiscriminatorOptions( insert = false )
	public static class Node {
		@EmbeddedId
		private NodeId id;

		private String name;

		protected Node() {
		}

		public Node(NodeId id, String name) {
			this.id = id;
			this.name = name;
		}

		public NodeId getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Embeddable
	public static class NodeId implements Serializable {
		private Long id;

		@Column( name = "type" )
		private String type;

		public NodeId() {
		}

		public NodeId(Long id, String type) {
			this.id = id;
			this.type = type;
		}
	}

	@Entity( name = "NodeA" )
	@DiscriminatorValue( "NODEA" )
	public static class NodeA extends Node {
		protected NodeA() {
		}

		public NodeA(NodeId nodeId, String name) {
			super( nodeId, name );
		}
	}

	@Entity( name = "NodeB" )
	@DiscriminatorValue( "NODEB" )
	public static class NodeB extends Node {
		protected NodeB() {
		}

		public NodeB(NodeId nodeId, String name) {
			super( nodeId, name );
		}
	}
}
