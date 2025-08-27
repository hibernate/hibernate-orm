/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cuk;//package com.example.demo;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@SessionFactory
@DomainModel(annotatedClasses = {
		JoinColumnOccursOutOfOrderTest.MasterEntity.class,
		JoinColumnOccursOutOfOrderTest.ChildTwoEntity.class,
		JoinColumnOccursOutOfOrderTest.ChildEntityOne.class,
		JoinColumnOccursOutOfOrderTest.ChildEntityFour.class,
		JoinColumnOccursOutOfOrderTest.ChildEntityThree.class,
})
@JiraKey("HHH-16263")
public class JoinColumnOccursOutOfOrderTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var meta = new MasterEntity();
			meta.setId(1l);
			meta.column1 = "1";
			meta.column2 = "2";
			meta.column3 = "3";
			session.persist( meta );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
		} );
	}

	@Test
	public void testOutOffOrder(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var container = session.createQuery(
					"from MasterEntity container where container.id = :param ",
					MasterEntity.class
			).setParameter("param", 1).getSingleResult();
		} );
	}

	@Embeddable
	public static class ChildOneId implements Serializable {

		@Column(name = "COLUMN_1", nullable = false, length = 7)
		private String column1;

		@Column(name = "COLUMN_2", nullable = false, length = 4)
		private String column2;

		@Column(name = "COLUMN_3", nullable = false, length = 2)
		private String column3;
	}

	@Embeddable
	public static class ChildTwoId implements Serializable {

		@Column(name = "COLUMN_1", length = 7, nullable = false)
		private String column1;
		@Column(name = "COLUMN_2", length = 4, nullable = false)
		private String column2;
		@Column(name = "COLUMN_3", length = 2, nullable = false)
		private String column3;

	}

	@Embeddable
	public static class ChildThreeId implements Serializable {

		@Column(name = "COLUMN_1", nullable = false, length = 7)
		private String column1;

		@Column(name = "COLUMN_2", nullable = false, length = 4)
		private String column2;

		@Column(name = "COLUMN_3", nullable = false, length = 2)
		private String column3;

		@Column(name = "COLUMN_4", nullable = false)
		private LocalDateTime creation;

	}

	@Embeddable
	public static class ChildFourId implements Serializable {

		@Column(name = "COLUMN_1", length = 7, nullable = false)
		private String column1;

		@Column(name = "COLUMN_2", length = 4, nullable = false)
		private String colum2;

		@Column(name = "COLUMN_3", length = 2, nullable = false)
		private String column3;

		@Column(name = "NON_UNIQUE_ID", nullable = false)
		private Long nonUNiqueId;

		@Column(name = "COLUMN_4", nullable = false)
		private LocalDateTime creation;

	}

	@Entity(name = "ChildEntityFour")
	public static class ChildEntityFour {
		@EmbeddedId
		private ChildFourId id;
	}

	@Entity(name = "ChildEntityOne")
	public static class ChildEntityOne {
		@EmbeddedId
		private ChildOneId id;
	}

	@Entity(name = "ChildTwoEntity")
	@DynamicUpdate
	public static class ChildTwoEntity {
		@EmbeddedId
		private ChildTwoId id;
	}

	@Entity(name = "ChildEntityThree")
	public static class ChildEntityThree  {
		@EmbeddedId
		private ChildThreeId id;
	}


	@Entity(name = "MasterEntity")
	public static class MasterEntity {

		@Id
		@Column(name = "ID", unique = true, nullable = false)
		private Long id;

		@Column(name = "COLUMN_1", nullable = false, length = 7)
		private String column1;

		@Column(name = "COLUMN_2", nullable = false, length = 4)
		private String column2;

		@Column(name = "COLUMN_3", nullable = false, length = 2)
		private String column3;

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
		@JoinColumns(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), value = {
				@JoinColumn(name = "COLUMN_1", referencedColumnName = "COLUMN_1", insertable = false, updatable = false),
				@JoinColumn(name = "COLUMN_2", referencedColumnName = "COLUMN_2", insertable = false, updatable = false),
				@JoinColumn(name = "COLUMN_3", referencedColumnName = "COLUMN_3", insertable = false, updatable = false)
		})
		@NotFound(action = NotFoundAction.IGNORE)
		private ChildEntityOne childEntityOne;

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
		@JoinColumns(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), value = {
				@JoinColumn(name = "COLUMN_1", referencedColumnName = "COLUMN_1", insertable = false, updatable = false),
				@JoinColumn(name = "COLUMN_2", referencedColumnName = "COLUMN_2", insertable = false, updatable = false),
				@JoinColumn(name = "COLUMN_3", referencedColumnName = "COLUMN_3", insertable = false, updatable = false)
		})
		@NotFound(action = NotFoundAction.IGNORE)
		private ChildTwoEntity childEntityTwo;

		@OneToMany(fetch = FetchType.LAZY)
		@JoinColumn(name = "COLUMN_1", referencedColumnName = "COLUMN_1", insertable = false, updatable = false)
		@JoinColumn(name = "COLUMN_2", referencedColumnName = "COLUMN_2", insertable = false, updatable = false)
		@JoinColumn(name = "COLUMN_3", referencedColumnName = "COLUMN_3", insertable = false, updatable = false)
		private List<ChildEntityThree> childEntityThree;

		@OneToMany(fetch = FetchType.LAZY)
		@JoinColumn(name = "COLUMN_1", referencedColumnName = "COLUMN_1", insertable = false, updatable = false)
		@JoinColumn(name = "COLUMN_2", referencedColumnName = "COLUMN_2", insertable = false, updatable = false)
		@JoinColumn(name = "COLUMN_3", referencedColumnName = "COLUMN_3", insertable = false, updatable = false)
		private List<ChildEntityFour> childEntityFour;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

	}

}
