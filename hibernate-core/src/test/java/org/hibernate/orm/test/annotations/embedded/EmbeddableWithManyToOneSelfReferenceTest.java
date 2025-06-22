/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embedded;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DomainModel(
		annotatedClasses = {
				EmbeddableWithManyToOneSelfReferenceTest.EntityTest.class,
				EmbeddableWithManyToOneSelfReferenceTest.IntIdEntity.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
public class EmbeddableWithManyToOneSelfReferenceTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					IntIdEntity intIdEntity = new IntIdEntity( 1 );

					EntityTest entity1 = new EntityTest( "1", intIdEntity );
					EmbeddableTest embeddable1 = new EmbeddableTest();
					embeddable1.setName( "E1" );

					entity1.setEmbeddedAttribute( embeddable1 );

					EntityTest entity2 = new EntityTest( "2", intIdEntity );

					EmbeddableTest embeddable2 = new EmbeddableTest();
					embeddable2.setAssociation( entity1 );
					embeddable2.setName( "E2" );

					entity2.setEmbeddedAttribute( embeddable2 );

					session.persist( intIdEntity );
					session.persist( entity1 );
					session.persist( entity2 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					EntityTest entity1 = session.find( EntityTest.class, new EntityTestId( "1", session.getReference( IntIdEntity.class, 1 ) ) );
					assertNotNull( entity1.getEmbeddedAttribute() );
					assertNull( entity1.getEmbeddedAttribute().getAssociation() );
					assertEquals( "E1", entity1.getEmbeddedAttribute().getName() );

					EntityTest entity2 = session.find( EntityTest.class, new EntityTestId( "2", session.getReference( IntIdEntity.class, 1 ) ) );
					assertNotNull( entity2.getEmbeddedAttribute() );
					assertNotNull( entity2.getEmbeddedAttribute().getAssociation() );
					assertEquals( entity1, entity2.getEmbeddedAttribute().getAssociation() );
					assertEquals( "E2", entity2.getEmbeddedAttribute().getName() );
				}
		);
	}

	@Entity(name = "EntityTest")
	public static class EntityTest {
		@EmbeddedId
		private EntityTestId id;

		@Embedded
		private EmbeddableTest embeddedAttribute;

		public EntityTest() {
		}

		public EntityTest(String string, IntIdEntity intIdEntity) {
			this.id = new EntityTestId(string, intIdEntity);
		}

		public EntityTestId getId() {
			return id;
		}

		public void setId(EntityTestId id) {
			this.id = id;
		}

		public EmbeddableTest getEmbeddedAttribute() {
			return embeddedAttribute;
		}

		public void setEmbeddedAttribute(EmbeddableTest embeddedAttribute) {
			this.embeddedAttribute = embeddedAttribute;
		}
	}

	@Embeddable
	public static class EntityTestId {
		@Column(name = "string_key", length = 10)
		private String stringKey;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "intIdEntity_id")
		private IntIdEntity intIdEntity;

		public EntityTestId() {
		}

		public EntityTestId(String stringKey, IntIdEntity intIdEntity) {
			this.stringKey = stringKey;
			this.intIdEntity = intIdEntity;
		}

		public String getStringKey() {
			return stringKey;
		}

		public void setStringKey(String stringField) {
			this.stringKey = stringField;
		}

		public IntIdEntity getIntIdEntity() {
			return intIdEntity;
		}

		public void setIntIdEntity(IntIdEntity entity) {
			this.intIdEntity = entity;
		}
	}

	@Embeddable
	public static class EmbeddableTest {
		private String name;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumns({
				@JoinColumn(name = "assoc_string_key", referencedColumnName = "string_key"),
				@JoinColumn(name = "assoc_intIdEntity_id", referencedColumnName = "intIdEntity_id")
		})
		private EntityTest association;

		public String getName() {
			return name;
		}

		public void setName(String stringField) {
			this.name = stringField;
		}

		public EntityTest getAssociation() {
			return association;
		}

		public void setAssociation(EntityTest entity) {
			this.association = entity;
		}
	}

	@Entity(name = "IntIdEntity")
	public static class IntIdEntity {
		@Id
		private Integer id;

		private String name;

		public IntIdEntity() {
		}

		public IntIdEntity(int id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
