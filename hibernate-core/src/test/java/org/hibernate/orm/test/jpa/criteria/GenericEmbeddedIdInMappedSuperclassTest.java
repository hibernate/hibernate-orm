/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.criteria;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;

@Jpa(
		annotatedClasses = { GenericEmbeddedIdInMappedSuperclassTest.FirstEntity.class, GenericEmbeddedIdInMappedSuperclassTest.SecondEntity.class}
)
public class GenericEmbeddedIdInMappedSuperclassTest {

	@Test
	public void metamodelTest(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					assertEmbeddedIdPropertyExistsInMetamodel(scope, FirstEntity.class, "firstId");
					assertEmbeddedIdPropertyExistsInMetamodel(scope, SecondEntity.class, "secondId");
				}
		);
	}

	@Test
	public void criteriaBuilderTest(EntityManagerFactoryScope scope) throws Exception {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();

					assertCriteriaQueryCanAccessEmbeddedIdProperty(builder, FirstEntity.class, "firstId");
					assertCriteriaQueryCanAccessEmbeddedIdProperty(builder, SecondEntity.class, "secondId");
				}
		);
	}

	private void assertEmbeddedIdPropertyExistsInMetamodel(EntityManagerFactoryScope scope, Class<?> entityClass, String embeddedIdProperty) {
		EntityType<?> secondEntity = scope.getEntityManagerFactory().getMetamodel().entity( entityClass);
		EmbeddableType<?> idType = (EmbeddableType<?>) secondEntity.getSupertype().getIdType();
		try {
			Assertions.assertNotNull( idType.getAttribute( embeddedIdProperty));
		} catch (IllegalArgumentException e) {
			Assertions.fail("Couldn't find expected attribute " + embeddedIdProperty + " in embeddable idType");
		}
	}

	private  <T> void assertCriteriaQueryCanAccessEmbeddedIdProperty(CriteriaBuilder builder, Class<T> entityType, String embeddedIdProperty) {
		CriteriaQuery<T> criteria = builder.createQuery( entityType);
		Root<T> root = criteria.from( entityType);
		criteria.select(root);
		try {
			criteria.where(builder.equal(root.get("id").get(embeddedIdProperty), "abcde"));
		} catch (IllegalArgumentException e) {
			Assertions.fail("Couldn't find expected attribute id." + embeddedIdProperty);
		}

	}

	/**
	 * Abstract class for all entities with a custom (embedded) entity key.
	 */
	@MappedSuperclass
	public abstract class AbstractEmbeddedIdEntity<ID extends Serializable> {

		@EmbeddedId
		private ID id;

		public AbstractEmbeddedIdEntity() {
			id = newId();
		}

		public ID getId() {
			return id;
		}

		public void setId(ID id) {
			this.id = id;
		}

		protected abstract ID newId();
	}

	@Embeddable
	public class FirstEntityId implements Serializable {
		private static final long serialVersionUID = -3906287313997919890L;

		@Column(name = "firstId")
		private String firstId;

		public FirstEntityId() {
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (!(obj instanceof FirstEntityId)) {
				return false;
			}
			FirstEntityId other = (FirstEntityId) obj;
			return
					Objects.equals(firstId, other.firstId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(firstId);
		}
	}

	@Embeddable
	public class SecondEntityId implements Serializable {

		private static final long serialVersionUID = -6222219927968609616L;
		@Column(name = "secondId")
		private String secondId;

		public SecondEntityId() {
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (!(obj instanceof SecondEntityId)) {
				return false;
			}
			SecondEntityId other = (SecondEntityId) obj;
			return Objects.equals(secondId, other.secondId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(secondId);
		}
	}

	@Entity
	@Table(name = "firstEntity")
	public class FirstEntity extends AbstractEmbeddedIdEntity<FirstEntityId> {

		public FirstEntity() {
		}

		@Override
		protected FirstEntityId newId() {
			return new FirstEntityId();
		}

	}

	@Entity
	@Table(name = "secondEntity")
	public class SecondEntity extends AbstractEmbeddedIdEntity<SecondEntityId> {

		public SecondEntity() {
		}

		@Override
		protected SecondEntityId newId() {
			return new SecondEntityId();
		}

	}

}
