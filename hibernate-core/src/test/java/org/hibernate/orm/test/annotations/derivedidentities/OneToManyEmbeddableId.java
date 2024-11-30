/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-18702")
@DomainModel(
		annotatedClasses = {
				OneToManyEmbeddableId.Parent.class,
				OneToManyEmbeddableId.FirstChild.class,
				OneToManyEmbeddableId.SecondChild.class,
		}
)
@SessionFactory
public class OneToManyEmbeddableId {
	private static final BigDecimal FIRST_CHILD_CODE = new BigDecimal( 2 );

	@BeforeAll
	public static void init(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( BigDecimal.TEN, "Lio" );
					FirstChild firstChild = new FirstChild( parent, BigDecimal.ONE, FIRST_CHILD_CODE );
					SecondChild secondChild = new SecondChild( firstChild, BigDecimal.TEN, "Al" );
					firstChild.addChild( secondChild );
					session.persist( parent );
					session.persist( firstChild );
					session.persist( secondChild );
				}
		);
	}

	@Test
	public void propertyNavigationTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					FirstChild firstChild = session.createQuery(
									"select f from FirstChild f where f.code = :code", FirstChild.class )
							.setParameter( "code", FIRST_CHILD_CODE ).getSingleResult();

					assertThat( firstChild ).isNotNull();

					assertThat( firstChild.getChildren() ).hasSize( 1 );
				}
		);

	}

	@Entity(name = "TestEntity")
	public static class Parent {
		@Id
		private BigDecimal id;

		private String name;

		public Parent() {
		}

		public Parent(BigDecimal id, String name) {
			this.id = id;
			this.name = name;
		}

		@OneToMany(mappedBy = "id.parent")
		private List<FirstChild> children = new ArrayList<>();

		void addChild(FirstChild firstChild) {
			children.add( firstChild );
		}
	}

	@Entity(name = "FirstChild")
	public static class FirstChild {
		@EmbeddedId
		private FirstChildId id;

		@Column(name = "FIRST_CHILD_CODE")
		private BigDecimal code;

		@OneToMany(mappedBy = "id.firstChild")
		private List<SecondChild> children = new ArrayList<>();

		public FirstChildId getId() {
			return id;
		}

		public FirstChild() {
		}

		public FirstChild(Parent parent, BigDecimal bigDecimalNum, BigDecimal code) {
			this.id = new FirstChildId( bigDecimalNum, parent );
			parent.addChild( this );
			this.code = code;
		}

		public List<SecondChild> getChildren() {
			return children;
		}

		void addChild(SecondChild child) {
			children.add( child );
		}
	}

	@Embeddable
	public static class FirstChildId {
		private BigDecimal bigDecimalNum;

		@ManyToOne()
		private Parent parent;

		public FirstChildId() {
		}

		public FirstChildId(BigDecimal bigDecimalNum, Parent parent) {
			this.bigDecimalNum = bigDecimalNum;
			this.parent = parent;
		}
	}

	@Entity(name = "SecondChild")
	public static class SecondChild {
		@EmbeddedId
		private SecondChildId id;

		private String name;

		public SecondChild() {
		}

		public SecondChild(FirstChild firstChild, BigDecimal bigDecimalNum, String name) {
			this.id = new SecondChildId( bigDecimalNum, firstChild );
			this.name = name;
		}
	}

	@Embeddable
	public static class SecondChildId {
		private BigDecimal bigDecimalNum;

		@ManyToOne
		@JoinColumn(name = "FIRST_CHILD_CODE", referencedColumnName = "FIRST_CHILD_CODE")
		private FirstChild firstChild;

		public SecondChildId() {
		}

		public SecondChildId(BigDecimal bigDecimalNum, FirstChild firstChild) {
			this.bigDecimalNum = bigDecimalNum;
			this.firstChild = firstChild;
		}
	}

}
