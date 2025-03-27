/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.iterate;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@DomainModel(
		annotatedClasses = {
				ForwardOnlyScrollNoTransactionTest.Father.class,
				ForwardOnlyScrollNoTransactionTest.Son.class
		}
)
@SessionFactory
@Jira("HHH-17826")
public class ForwardOnlyScrollNoTransactionTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Son son = new Son( "Andrea" );
					Father father = new Father( "Lionello" );
					father.addSon( son );
					session.persist( father );

					Son son1 = new Son( "Alberto" );
					Father father1 = new Father( "Roberto" );
					father1.addSon( son1 );
					session.persist( father1 );
				}
		);
	}

	@Test
	public void testScroll(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Query query = session.createQuery( "select f from Father f" );

					try (ScrollableResults result = query.scroll( ScrollMode.FORWARD_ONLY )) {
						while ( result.next() ) {
							result.get();
						}
					}
				}
		);
	}

	@Entity(name = "Father")
	public static class Father {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		@OneToMany(mappedBy = "father", fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
		public List<Son> sons = new ArrayList<>();

		public Father() {
		}

		public Father(String name) {
			this.name = name;
		}

		public void addSon(Son son) {
			sons.add( son );
			son.father = this;
		}
	}

	@Entity(name = "Son")
	public static class Son {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		@ManyToOne
		private Father father;

		public Son() {
		}

		public Son(String name) {
			this.name = name;
		}
	}
}
