/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoone;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.transaction.TransactionUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-13044")
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
public class ManyToOneMapsIdFlushModeTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ParentEntity.class, ChildEntity.class };
	}

	@Test
	public void testFlushModeCommitWithMapsIdAndIdentity() {
		final ParentEntity parent = TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.setFlushMode( FlushModeType.COMMIT );

			final ParentEntity parentEntity = new ParentEntity();
			parentEntity.setData( "test" );

			final ChildEntity childEntity = new ChildEntity();
			parentEntity.addChild( childEntity );

			entityManager.persist( parentEntity );
			entityManager.persist( childEntity );

			return parentEntity;
		} );

		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final ParentEntity parentEntity = entityManager.find( ParentEntity.class, parent.getId() );
			assertNotNull( parentEntity );
			assertNotNull( parentEntity.getChildren() );
			assertTrue( !parentEntity.getChildren().isEmpty() );

			final ChildEntity childEntity = parentEntity.getChildren().iterator().next();
			assertNotNull( childEntity );
			assertEquals( parentEntity.getId(), childEntity.getId() );
		} );
	}

	@Entity(name = "ParentEntity")
	public static class ParentEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		private String data;
		@OneToMany(mappedBy = "parent")
		private Set<ChildEntity> children = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}

		public Set<ChildEntity> getChildren() {
			return children;
		}

		public void setChildren(Set<ChildEntity> children) {
			this.children = children;
		}

		public void addChild(ChildEntity child) {
			getChildren().add( child );
			child.setParent( this );
		}
	}

	@Entity(name = "ChildEntity")
	public static class ChildEntity {
		@Id
		private Long id;

		@MapsId
		@ManyToOne(optional = false, targetEntity = ParentEntity.class)
		private ParentEntity parent;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public ParentEntity getParent() {
			return parent;
		}

		public void setParent(ParentEntity parent) {
			this.parent = parent;
		}
	}
}
