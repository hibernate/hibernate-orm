/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.Version;
import org.hibernate.InstantiationException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DomainModel(
		annotatedClasses = {
				SelectWithWrongResultTypeTest.Element.class,
				SelectWithWrongResultTypeTest.Node.class,
		}
)
@SessionFactory
@JiraKey("HHH-19868")
public class SelectWithWrongResultTypeTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Node basik = new Node( "Child" );
					basik.parent = new Node( "Parent" );
					basik.elements.add( new Element( basik ) );
					basik.elements.add( new Element( basik ) );
					basik.elements.add( new Element( basik ) );

					session.persist( basik );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	void testSelectWithWrongResultType(SessionFactoryScope scope) {
		assertThrows( InstantiationException.class, () -> scope.inTransaction( session -> {
			List<Node> resultList = session
					.createSelectionQuery( "select distinct n, e from Node n join n.elements e", Node.class )
					.getResultList();
			assertThat( resultList ).hasSize( 3 );
		} ) );

		assertThrows( InstantiationException.class, () -> scope.inTransaction( session -> {
			List<Node> resultList = session
					.createSelectionQuery( "select distinct n.id, e.id from Node n join n.elements e", Node.class )
					.getResultList();
			assertThat( resultList ).hasSize( 3 );
		} ) );

		assertThrows( InstantiationException.class, () -> scope.inTransaction( session -> {
			List<Node> resultList = session
					.createSelectionQuery(
							"select max(e.id), min(e.id), sum(e.id) from Node n join n.elements e group by n.id order by n.id",
							Node.class )
					.getResultList();
			assertThat( resultList ).hasSize( 1 );
		} ) );
	}

	@Test
	void testSelect(SessionFactoryScope scope) {

		scope.inTransaction( session -> {
			List<Node> resultList = session
					.createSelectionQuery( "select distinct n from Node n left join fetch n.elements", Node.class )
					.getResultList();
			assertThat( resultList ).hasSize( 2 );
		} );
		scope.inTransaction( session -> {
			List<Tuple> resultList = session
					.createSelectionQuery( "select distinct n, e from Node n join n.elements e", Tuple.class )
					.getResultList();
			assertThat( resultList ).hasSize( 3 );
		} );
		scope.inTransaction( session -> {
			List<Tuple> resultList = session
					.createSelectionQuery( "select distinct n.id, e.id from Node n join n.elements e", Tuple.class )
					.getResultList();
			assertThat( resultList ).hasSize( 3 );
		} );
		scope.inTransaction( session -> {
			List<Tuple> resultList = session
					.createSelectionQuery(
							"select max(e.id), min(e.id), sum(e.id) from Node n join n.elements e group by n.id order by n.id",
							Tuple.class )
					.getResultList();
			assertThat( resultList ).hasSize( 1 );
		} );
	}

	@Entity(name = "Element")
	@Table(name = "Element")
	public static class Element {
		@Id
		@GeneratedValue
		Integer id;

		@ManyToOne
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

		@ManyToOne(fetch = FetchType.LAZY,
				cascade = CascadeType.PERSIST)
		Node parent;

		@OneToMany(fetch = FetchType.EAGER,
				cascade = CascadeType.PERSIST,
				mappedBy = "node")
		List<Element> elements = new ArrayList<>();

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
