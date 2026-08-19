/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idclass;


import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey("HHH-20796")
@DomainModel(
	annotatedClasses = {LazyManyToOneWithRecordIdClassesTest.Parent.class, LazyManyToOneWithRecordIdClassesTest.Child.class}
)
@SessionFactory
public class LazyManyToOneWithRecordIdClassesTest {

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Parent parent = new Parent();
					parent.alpha = "a";
					parent.bravo = "b";
					parent.charlie = 0;
					session.persist(parent);
					final Child child = new Child();
					child.id = "c";
					child.parentAlpha = "a";
					child.parentBravo = "b";
					child.parentCharlie = 0;
					child.parent = parent;
					session.persist(child);
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void loadChildrenShouldNotThrow(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					final List<Child> children = session.createQuery("from Child", Child.class).list();
					assertEquals(1, children.size());
				}
		);
	}

	public record ChildId(String id, String parentAlpha, String parentBravo, int parentCharlie) implements Serializable {}

	@Entity(name = "Child")
	@IdClass(ChildId.class)
	public static class Child {
		@Id
		String id;
		@Id
		String parentAlpha;
		@Id
		String parentBravo;
		@Id
		int parentCharlie;
		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumns(
				{
						@JoinColumn(
								name = "parentAlpha",
								referencedColumnName = "alpha",
								insertable = false,
								updatable = false
						),
						@JoinColumn(
								name = "parentBravo",
								referencedColumnName = "bravo",
								insertable = false,
								updatable = false
						),
						@JoinColumn(
								name = "parentCharlie",
								referencedColumnName = "charlie",
								insertable = false,
								updatable = false
						)
				}
		)
		Parent parent;
	}

	public record ParentId(int charlie, String alpha, String bravo) implements Serializable {}

	@Entity(name = "Parent")
	@IdClass(ParentId.class)
	public static class Parent {
		@Id
		int charlie;
		@Id
		String alpha;
		@Id
		String bravo;
	}

}
