/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.persister.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

@JiraKey(value = "HHH-16979")
@Jpa(annotatedClasses = {
		JoinedInheritanceImplicitJoinTest.MasterEntity.class,
		JoinedInheritanceImplicitJoinTest.ChildEntity.class,
		JoinedInheritanceImplicitJoinTest.RelatedEntity.class
}, properties = {
		@Setting(name = AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, value = "true")
})
public class JoinedInheritanceImplicitJoinTest {

	@BeforeEach
	public void setup(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			final var childEntity = new ChildEntity();
			childEntity.setMasterField("masterValue001");
			childEntity.setChildField("childValue001");

			RelatedEntity relatedEntity = new RelatedEntity();
			relatedEntity.setRelatedField("relatedValue001");
			relatedEntity.setChildEntity(childEntity);

			RelatedEntity relatedEntity2 = new RelatedEntity();
			relatedEntity2.setRelatedField("relatedValue002");
			relatedEntity2.setChildEntity(childEntity);

			Set<RelatedEntity> relatedEntityHashSet = Set.of(relatedEntity, relatedEntity2);

			childEntity.setRelatedEntitySet(relatedEntityHashSet);

			entityManager.persist(childEntity);
			entityManager.persist(relatedEntity);
			entityManager.persist(relatedEntity2);
		});
	}

	@Test
	public void testImplicitJoinWithoutRelatedEntity(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createQuery(Tuple.class);
			Root<ChildEntity> root = query.from(ChildEntity.class);

			query.multiselect(
					root.get("masterField"),
					root.get("childField")
			);

			TypedQuery<Tuple> result = entityManager.createQuery(query);
			List<Tuple> resultList = result.getResultList();

			Assertions.assertFalse(resultList.isEmpty());
		});
	}

	@Test
	public void testImplicitJoinWithRelatedEntity(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createQuery(Tuple.class);
			Root<ChildEntity> root = query.from(ChildEntity.class);
			Join<ChildEntity, RelatedEntity> relatedEntityJoin = root.join("relatedEntitySet");

			query.multiselect(
					root.get("masterField"),
					root.get("childField"),
					relatedEntityJoin.get("relatedField")
			);

			TypedQuery<Tuple> result = entityManager.createQuery(query);
			List<Tuple> resultList = result.getResultList();

			Assertions.assertFalse(resultList.isEmpty());
		});
	}

	@Entity(name = "MasterEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class MasterEntity {
		@Id
		@GeneratedValue
		public Long id;

		public String masterField;


		public Long getId() {
			return id;
		}

		public String getMasterField() {
			return masterField;
		}

		public void setMasterField(final String masterField) {
			this.masterField = masterField;
		}
	}

	@Entity(name = "ChildEntity")
	public static class ChildEntity extends MasterEntity {
		private String childField;

		@OneToMany(mappedBy = "childEntity")
		public Set<RelatedEntity> relatedEntitySet;

		public String getChildField() {
			return childField;
		}

		public void setChildField(final String childField) {
			this.childField = childField;
		}

		public Set<RelatedEntity> getRelatedEntitySet() {
			return relatedEntitySet;
		}

		public void setRelatedEntitySet(final Set<RelatedEntity> relatedEntitySet) {
			this.relatedEntitySet = relatedEntitySet;
		}
	}

	@Entity(name = "RelatedEntity")
	public static class RelatedEntity {
		@Id
		@GeneratedValue
		public Long id;

		public String relatedField;

		@ManyToOne
		public ChildEntity childEntity;

		public Long getId() {
			return id;
		}

		public String getRelatedField() {
			return relatedField;
		}

		public void setRelatedField(final String relatedField) {
			this.relatedField = relatedField;
		}

		public ChildEntity getChildEntity() {
			return childEntity;
		}

		public void setChildEntity(final ChildEntity childEntity) {
			this.childEntity = childEntity;
		}
	}
}
