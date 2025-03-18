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
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11133")
public class DiscriminatorJoinedInheritanceTest extends BaseEnversJPAFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ParentEntity.class, ChildEntity.class, ChildListHolder.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager entityManager = getEntityManager();
		try {
			ChildEntity childEntity = new ChildEntity( 1, "Child" );
			entityManager.getTransaction().begin();
			entityManager.persist( childEntity );
			entityManager.getTransaction().commit();

			ChildListHolder holder = new ChildListHolder();
			holder.setId( 1 );
			holder.setChildren( Arrays.asList( childEntity ) );
			entityManager.getTransaction().begin();
			entityManager.persist( holder );
			entityManager.getTransaction().commit();

		}
		catch ( Exception e ) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( ChildEntity.class, 1 ) );
		assertEquals( Arrays.asList( 2 ), getAuditReader().getRevisions( ChildListHolder.class, 1 ) );
	}

	@Test
	public void testConfiguredDiscriminatorValue() {
		ChildEntity entity = getAuditReader().find( ChildEntity.class, 1, 1 );
		assertEquals( "ce", entity.getType() );
	}

	@Test
	public void testDiscriminatorValuesViaRelatedEntityQuery() {
		ChildListHolder holder = getAuditReader().find( ChildListHolder.class, 1, 2 );
		assertEquals( 1, holder.getChildren().size() );
		assertEquals( "ce", holder.getChildren().get( 0 ).getType() );
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
