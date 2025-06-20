/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.io.Serializable;
import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = {
				SelectManyToOneEmbeddedIdWithKeyManyToOneQueryTest.EmbeddableTestEntity.class,
				SelectManyToOneEmbeddedIdWithKeyManyToOneQueryTest.IntIdEntity.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-15339")
public class SelectManyToOneEmbeddedIdWithKeyManyToOneQueryTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EmbeddableTestEntity entity1, entity2;

					IntIdEntity intIdEntity1 = new IntIdEntity("1");
					entity1 = new EmbeddableTestEntity();
					entity1.setId(new EmbeddableTestEntityId( intIdEntity1, "1"));

					IntIdEntity intIdEntity2 = new IntIdEntity("2");
					entity2 = new EmbeddableTestEntity();
					entity2.setId(new EmbeddableTestEntityId( intIdEntity2, "2"));
					entity2.setManyToOne(entity1);

					session.persist(intIdEntity1);
					session.persist(intIdEntity2);
					session.persist(entity1);
					session.persist(entity2);
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSelectManyToOneEmbeddedIdWithKeyManyToOne(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery(
							"SELECT e.manyToOne.id " +
									"FROM EmbeddableTestEntity e ",
							Tuple.class).list();
					List<Tuple> result = session.createQuery(
									"SELECT e.manyToOne.id, i " +
											"FROM EmbeddableTestEntity e " +
											"JOIN e.manyToOne m " +
											"LEFT JOIN m.id.intIdEntity i",
									Tuple.class)
							.list();
					assertThat( result.size(), is( 1 ) );
					assertEquals(
							result.get( 0 ).get( 0, EmbeddableTestEntityId.class ).getIntIdEntity(),
							result.get( 0 ).get( 1 )
					);
				}
		);
	}

	@Entity(name = "EmbeddableTestEntity")
	@Table(name = "ent")
	public static class EmbeddableTestEntity implements Serializable {

		private static final long serialVersionUID = 1L;

		private EmbeddableTestEntityId id = new EmbeddableTestEntityId();
		private EmbeddableTestEntity manyToOne;

		public EmbeddableTestEntity() {
		}

		@EmbeddedId
		public EmbeddableTestEntityId getId() {
			return id;
		}

		public void setId(EmbeddableTestEntityId id) {
			this.id = id;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "assoc_key", referencedColumnName = "id_key")
		@JoinColumn(name = "assoc_fk", referencedColumnName = "id_fk")
		public EmbeddableTestEntity getManyToOne() {
			return manyToOne;
		}

		public void setManyToOne(EmbeddableTestEntity manyToOne) {
			this.manyToOne = manyToOne;
		}

	}

	@Embeddable
	public static class EmbeddableTestEntityId implements Serializable {

		private static final long serialVersionUID = 1L;

		private IntIdEntity intIdEntity;
		private String key;

		public EmbeddableTestEntityId() {
		}

		public EmbeddableTestEntityId(IntIdEntity intIdEntity, String key) {
			this.intIdEntity = intIdEntity;
			this.key = key;
		}

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "id_fk")
		public IntIdEntity getIntIdEntity() {
			return intIdEntity;
		}

		public void setIntIdEntity(IntIdEntity intIdEntity) {
			this.intIdEntity = intIdEntity;
		}

		@Column(name = "id_key", length = 100)
		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((intIdEntity == null) ? 0 : intIdEntity.hashCode());
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EmbeddableTestEntityId other = (EmbeddableTestEntityId) obj;
			if (intIdEntity == null) {
				if (other.intIdEntity != null)
					return false;
			} else if (!intIdEntity.equals(other.intIdEntity))
				return false;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			return true;
		}

	}

	@Entity(name = "IntIdEntity")
	@Table(name = "id_fkity")
	public static class IntIdEntity implements Serializable {
		private static final long serialVersionUID = 1L;

		private Integer id;
		private String name;
		private Integer value;

		public IntIdEntity() {
		}

		public IntIdEntity(String name) {
			this.name = name;
		}

		@Id
		@GeneratedValue
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Basic(optional = false)
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Column(name = "val")
		public Integer getValue() {
			return value;
		}

		public void setValue(Integer value) {
			this.value = value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof IntIdEntity)) {
				return false;
			}
			IntIdEntity other = (IntIdEntity) obj;
			if (getId() == null) {
				if (other.getId() != null) {
					return false;
				}
			} else if (!getId().equals(other.getId())) {
				return false;
			}
			return true;
		}
	}



}
