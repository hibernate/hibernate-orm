/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.refcolnames.misc;

import jakarta.persistence.Basic;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@DomainModel(annotatedClasses = {Misc3Test.A.class, Misc3Test.B.class})
@SessionFactory
@JiraKey(value = "HHH-14687")
public class Misc3Test {

	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction(x -> {});
	}

	@Entity
	@Table(name = "A")
	public static final class A {
		@Id
		@GeneratedValue
		private Long id;
		@Basic
		private String name;
	}

	@Entity
	@Table(name = "B", uniqueConstraints = {@UniqueConstraint(columnNames = {"a_id", "uniqueName"})})
	public static final class B {
		@Id
		@GeneratedValue
		private Long id;

		@Basic
		private String uniqueName;
		@ManyToOne
		@JoinColumn(name="a_id", referencedColumnName="id")
		private A a;
	}

	@Entity
	@Table(name = "C", uniqueConstraints = {@UniqueConstraint(columnNames = {"a_id", "uniqueName"})})
	public static final class C {
		@Id
		@GeneratedValue
		private Long id;

		@Basic
		private String uniqueName;

		@ManyToOne
		@JoinColumn(name="a_id", referencedColumnName="id")
		private A a;
		@ManyToOne
		@JoinColumns(
				value = {
						@JoinColumn(name = "uniqueName", referencedColumnName = "uniqueName", insertable = false, updatable = false),
						@JoinColumn(name = "a_id", referencedColumnName = "a_id", insertable = false, updatable = false)
				},
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		private B b;
	}
}
