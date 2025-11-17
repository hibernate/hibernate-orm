/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.time.LocalDate;

import org.hibernate.community.dialect.DerbyDialect;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Query;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				QueryComparingAssociationToNullTest.Parent.class,
				QueryComparingAssociationToNullTest.Child.class,
		}
)
@SessionFactory
@JiraKey("HHH-16974")
@SkipForDialect( dialectClass = DerbyDialect.class, reason = "it does not like '= null'")
@SkipForDialect( dialectClass = InformixDialect.class, reason = "it does not like '= null'")
public class QueryComparingAssociationToNullTest {

	@Test
	public void testQuery(SessionFactoryScope scope) {
		LocalDate date = LocalDate.now();
		scope.inTransaction(
				session -> {
					Child child = new Child( 1, "first" );
					Parent parent = new Parent( 1, date, child );
					session.persist( child );
					session.persist( parent );
				}
		);

		scope.inTransaction(
				session -> {
					Query query = session.createQuery(
							"SELECT p  FROM Parent p  WHERE p.child = NULL ",
							Parent.class
					);
					query.getResultList();
				}
		);

		scope.inTransaction(
				session -> {
					Query query = session.createQuery(
							"SELECT p  FROM Parent p  WHERE p.child = NULL OR p.date = :date  ",
							Parent.class
					).setParameter( "date", date );
					assertThat(query.getResultList().size()).isEqualTo( 1 );
				}
		);

		scope.inTransaction(
				session -> {
					Query query = session.createQuery(
							"SELECT p  FROM Parent p  WHERE p.child = NULL OR p.date = NULL  ",
							Parent.class
					);
					query.getResultList();
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		private Integer id;

		@Column(name = "COLUMN_DATE")
		private LocalDate date;

		@ManyToOne
		private Child child;


		public Parent() {
		}

		public Parent(Integer id, LocalDate date, Child child) {
			this.id = id;
			this.date = date;
			this.child = child;
		}
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		private Integer id;

		private String name;

		public Child() {
		}

		public Child(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

}
