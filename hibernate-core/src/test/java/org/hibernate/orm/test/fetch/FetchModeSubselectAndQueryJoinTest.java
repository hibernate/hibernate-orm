/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetch;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				FetchModeSubselectAndQueryJoinTest.Node.class,
				FetchModeSubselectAndQueryJoinTest.Element.class
		}
)
@SessionFactory
@JiraKey( "HHH-19949" )
public class FetchModeSubselectAndQueryJoinTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Node basik = new Node( "Child" );
					basik.elements.add( new Element( basik ) );
					basik.elements.add( new Element( basik ) );
					session.persist( basik );
				}
		);
	}

	@Test
	public void tesSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Tuple> results = session.createSelectionQuery(
							"select distinct n, e from Node n join n.elements e ", Tuple.class ).getResultList();
					assertThat( results ).hasSize( 2 );
				}
		);
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

		Element() {
		}
	}

	@Entity(name = "Node")
	@Table(name = "Node")
	public static class Node {

		@Id
		@GeneratedValue
		Integer id;
		String string;

		@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST, mappedBy = "node")
		@Fetch(FetchMode.SUBSELECT)
		List<Element> elements = new ArrayList<>();

		public Node(String string) {
			this.string = string;
		}

		Node() {
		}
	}
}
