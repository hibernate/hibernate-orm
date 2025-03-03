/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idclass;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = { IdClassWithSuperclassTest.MyEntity.class }
)
@SessionFactory
@JiraKey("HHH-16664")
public class IdClassWithSuperclassTest {

	@Test
	public void testSaveAndGet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					MyEntity myEntity = new MyEntity( 1, 2 );
					session.persist( myEntity );
				}
		);

		scope.inTransaction(
				session -> {
					MyEntity myEntity = session.get( MyEntity.class, new ChildPrimaryKey(  1, 2 ) );
					assertThat( myEntity ).isNotNull();
				}
		);
	}

	public static class ParentPrimaryKey implements Serializable {
		private Integer parentId;

		public ParentPrimaryKey() {
		}

		public ParentPrimaryKey(Integer parentId) {
			this.parentId = parentId;
		}
	}

	public static class ChildPrimaryKey extends ParentPrimaryKey{
		private Integer childId;

		public ChildPrimaryKey() {
		}

		public ChildPrimaryKey(Integer parentId, Integer childId) {
			super(parentId);
			this.childId = childId;
		}
	}

	@Entity(name = "MyEntity")
	@IdClass(ChildPrimaryKey.class)
	public static class MyEntity {

		@Id
		private Integer parentId;

		@Id
		private Integer childId;

		public MyEntity() {
		}

		public MyEntity(Integer parentId, Integer childId) {
			this.parentId = parentId;
			this.childId = childId;
		}

		public Integer getParentId() {
			return parentId;
		}

		public Integer getChildId() {
			return childId;
		}
	}
}
