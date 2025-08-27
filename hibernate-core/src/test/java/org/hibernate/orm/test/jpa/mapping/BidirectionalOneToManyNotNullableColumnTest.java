/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.mapping;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

@JiraKey(value = "HHH-13287")
@Jpa(annotatedClasses = {
		BidirectionalOneToManyNotNullableColumnTest.ParentData.class,
		BidirectionalOneToManyNotNullableColumnTest.ChildData.class
})
public class BidirectionalOneToManyNotNullableColumnTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}


	@Test
	@FailureExpected( jiraKey = "HHH-13287" )
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					ParentData parent = new ParentData();
					parent.setId( 1L );
					parent.addChildData( new ChildData() );
					parent.addChildData( new ChildData() );

					entityManager.persist( parent );
				}
		);

		scope.inTransaction(
				entityManager -> {
					ParentData parent = entityManager.find( ParentData.class, 1L );

					assertSame( 2, parent.getChildren().size() );
				}
		);
	}

	@Entity(name = "ParentData")
	public static class ParentData {
		@Id
		long id;

		@OneToMany(mappedBy = "parentData", cascade = CascadeType.ALL, orphanRemoval = true)
		@OrderColumn(name = "listOrder", nullable = false)
		private List<ChildData> children = new ArrayList<>();

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public List<ChildData> getChildren() {
			return children;
		}

		public void addChildData(ChildData childData) {
			childData.setParentData( this );
			children.add( childData );
		}
	}

	@Entity(name = "ChildData")
	public static class ChildData {
		@Id
		@GeneratedValue
		long id;

		@ManyToOne
		private ParentData parentData;

		public ChildData() {
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public ParentData getParentData() {
			return parentData;
		}

		public void setParentData(ParentData parentData) {
			this.parentData = parentData;
		}
	}
}
