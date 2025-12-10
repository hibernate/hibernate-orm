/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.FetchType.EAGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.annotations.FetchMode.JOIN;
import static org.hibernate.annotations.FetchMode.SUBSELECT;

@DomainModel(
		annotatedClasses = {
				SubselectFetch2Test.NodeHolder.class,
				SubselectFetch2Test.NodeIntermediateHolder.class,
				SubselectFetch2Test.Element.class,
				SubselectFetch2Test.Node.class,
		}
)
@SessionFactory
public class SubselectFetch2Test {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Node basik = new Node( "Child" );
					Element n1, n2;
					basik.elements.add( n1 = new Element( basik ) );
					basik.elements.add( n2 = new Element( basik ) );
					basik.elements.add( new Element( basik ) );

					Node node2 = new Node( "Child2" );
					node2.parent = new NodeIntermediateHolder( basik );
					node2.parent.strings = new ArrayList<>( Arrays.asList("s1", "s2"));
					node2.elements.add( n1 );
					node2.elements.add( n2 );
					node2.elements.add( new Element( basik ) );

					NodeHolder root = new NodeHolder( basik, node2 );
					session.persist( root );

					session.persist( new NodeHolder( null, null ) );
				}
		);
	}

	@AfterEach
	@JiraKey("HHH-19868")
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	void test2(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			NodeHolder holder = session.createSelectionQuery( "from NodeHolder nh join fetch nh.node1 join fetch nh.node2", NodeHolder.class ).getSingleResult();
			assertThat( holder.node1.elements ).hasSize( 3 );
			assertThat( holder.node2.elements ).hasSize( 3 );
		} );
	}

	@Test
	void test3(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createSelectionQuery( "select nh, n1, n2 from NodeHolder nh join nh.node1 n1 join  nh.node2 n2" ).getResultList();
		} );
	}

	@Entity(name = "NodeHolder")
	public static class NodeHolder {
		@Id
		@GeneratedValue
		Integer id;

		@ManyToOne(cascade =  PERSIST)
		Node node1;
		@ManyToOne(cascade =  PERSIST)
		Node node2;

		public NodeHolder(Node node1, Node node2) {
			this.node1 = node1;
			this.node2 = node2;
		}

		public NodeHolder() {
		}
	}


	@Entity(name = "NodeIntermediateHolder")
	public static class NodeIntermediateHolder {
		@Id
		@GeneratedValue
		Integer id;

		@ManyToOne(cascade =  PERSIST)
		Node node;

		@ElementCollection(fetch = EAGER)
		@Fetch( JOIN )
		List<String> strings;

		public NodeIntermediateHolder(Node node) {
			this.node = node;
		}

		public NodeIntermediateHolder() {
		}
	}

	@Entity(name = "Element")
	@Table(name = "Element")
	public static class Element {
		@Id
		@GeneratedValue
		Integer id;

		@ManyToOne
		@Fetch(FetchMode.SELECT)
		Node node;

		public Element(Node node) {
			this.node = node;
		}

		public Element() {
		}
	}

	@Entity(name = "Node")
	@Table(name = "Node")
	public static class Node {

		@Id
		@GeneratedValue
		Integer id;
		@Version
		Integer version;
		String string;
		@Transient
		boolean loaded = false;

		@ManyToOne(fetch = EAGER, cascade =  PERSIST)
		NodeIntermediateHolder parent;

		@ManyToMany(fetch = EAGER, cascade =  PERSIST)
		@Fetch(SUBSELECT)
		Set<Element> elements = new HashSet<>();

		public Node(String string) {
			this.string = string;
		}

		public Node() {
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getString() {
			return string;
		}

		public void setString(String string) {
			this.string = string;
		}

//		@PostLoad
//		void postLoad() {
//			loaded = true;
//		}

		@Override
		public String toString() {
			return id + ": " + string;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Node node = (Node) o;
			return Objects.equals( string, node.string );
		}

		@Override
		public int hashCode() {
			return Objects.hash( string );
		}
	}
}
