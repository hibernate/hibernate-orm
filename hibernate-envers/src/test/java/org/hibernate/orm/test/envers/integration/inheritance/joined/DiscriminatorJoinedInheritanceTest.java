/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.joined;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11133")
@EnversTest
@Jpa(annotatedClasses = {
		DiscriminatorJoinedInheritanceTest.ParentEntity.class,
		DiscriminatorJoinedInheritanceTest.ChildEntity.class,
		DiscriminatorJoinedInheritanceTest.ChildListHolder.class
})
public class DiscriminatorJoinedInheritanceTest {

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			ChildEntity childEntity = new ChildEntity( 1, "Child" );
			em.persist( childEntity );
		} );

		scope.inTransaction( em -> {
			ChildEntity childEntity = em.find( ChildEntity.class, 1 );
			ChildListHolder holder = new ChildListHolder();
			holder.setId( 1 );
			holder.setChildren( Arrays.asList( childEntity ) );
			em.persist( holder );
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( ChildEntity.class, 1 ) );
			assertEquals( Arrays.asList( 2 ), auditReader.getRevisions( ChildListHolder.class, 1 ) );
		} );
	}

	@Test
	public void testConfiguredDiscriminatorValue(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			ChildEntity entity = AuditReaderFactory.get( em ).find( ChildEntity.class, 1, 1 );
			assertEquals( "ce", entity.getType() );
		} );
	}

	@Test
	public void testDiscriminatorValuesViaRelatedEntityQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			ChildListHolder holder = AuditReaderFactory.get( em ).find( ChildListHolder.class, 1, 2 );
			assertEquals( 1, holder.getChildren().size() );
			assertEquals( "ce", holder.getChildren().get( 0 ).getType() );
		} );
	}

	@Entity(name = "ParentEntity")
	@Audited
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorValue("pe")
	@DiscriminatorColumn(name = "type", length = 255)
	public static abstract class ParentEntity {
		@Id
		private Integer id;

		@Column(insertable = false, updatable = false)
		private String type;

		ParentEntity() {

		}

		ParentEntity(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getType() {
			return type;
		}

		private void setType(String type) {
			this.type = type;
		}
	}

	@Entity(name = "ChildEntity")
	@Audited
	@DiscriminatorValue("ce")
	public static class ChildEntity extends ParentEntity {
		private String name;

		ChildEntity() {

		}

		ChildEntity(Integer id, String name) {
			super( id );
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "ChildListHolder")
	@Table(name = "CHILD_HOLDER")
	@Audited
	public static class ChildListHolder {
		@Id
		private Integer id;
		@OneToMany
		private List<ChildEntity> children;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<ChildEntity> getChildren() {
			return children;
		}

		public void setChildren(List<ChildEntity> children) {
			this.children = children;
		}
	}
}
