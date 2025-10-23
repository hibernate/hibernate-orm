/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import org.hibernate.Hibernate;
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
import java.util.List;
import java.util.Objects;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.FetchType.LAZY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.annotations.FetchMode.SUBSELECT;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				SubselectFetchTest.Element.class,
				SubselectFetchTest.Node.class,
		}
)
@SessionFactory
public class SubselectFetchTest {

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
	@JiraKey("HHH-19868")
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	void testEagerFetchQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Node> list = session.createSelectionQuery( "from Node order by id", Node.class ).getResultList();
			assertThat( list ).hasSize( 2 );
			assertThat( Hibernate.isInitialized( list.get( 0 ).elements ) ).isTrue();
			assertThat( list.get( 0 ).elements ).hasSize( 3 );
			assertThat( list.get( 1 ).elements ).isEmpty();
		} );

		scope.inTransaction( session -> {
			List<Object[]> list = session.createSelectionQuery( "select distinct n, e from Node n join n.elements e order by n.id", Object[].class ).getResultList();
			assertThat( list ).hasSize( 3 );
			Object[] tup = list.get( 0 );
			assertTrue( Hibernate.isInitialized( ( (Node) tup[0] ).elements ) );
			assertThat(  ( (Node) tup[0] ).elements ).hasSize( 3 );
		} );
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

		@ManyToOne(fetch = LAZY, cascade =  PERSIST)
		Node parent;

		@OneToMany(fetch = EAGER, cascade =  PERSIST , mappedBy = "node")
		@Fetch(SUBSELECT)
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
