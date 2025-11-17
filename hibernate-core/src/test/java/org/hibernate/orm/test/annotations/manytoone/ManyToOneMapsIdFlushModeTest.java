/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoone;

import jakarta.persistence.Entity;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Chris Cranford
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-13044")
@RequiresDialectFeature(feature= DialectFeatureChecks.SupportsIdentityColumns.class)
@DomainModel(annotatedClasses = { ManyToOneMapsIdFlushModeTest.ParentEntity.class, ManyToOneMapsIdFlushModeTest.ChildEntity.class })
@SessionFactory
public class ManyToOneMapsIdFlushModeTest {
	@Test
	public void testFlushModeCommitWithMapsIdAndIdentity(SessionFactoryScope factoryScope) {
		final ParentEntity parent = factoryScope.fromTransaction( (session) -> {
			session.setFlushMode( FlushModeType.COMMIT );

			var parentEntity = new ParentEntity();
			parentEntity.setData( "test" );

			var childEntity = new ChildEntity();
			parentEntity.addChild( childEntity );

			session.persist( parentEntity );
			session.persist( childEntity );

			return parentEntity;
		} );

		factoryScope.inTransaction( (session) -> {
			final ParentEntity parentEntity = session.find( ParentEntity.class, parent.getId() );
			Assertions.assertNotNull( parentEntity );
			Assertions.assertNotNull( parentEntity.getChildren() );
			Assertions.assertFalse( parentEntity.getChildren().isEmpty() );

			final ChildEntity childEntity = parentEntity.getChildren().iterator().next();
			Assertions.assertNotNull( childEntity );
			Assertions.assertEquals( parentEntity.getId(), childEntity.getId() );
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
