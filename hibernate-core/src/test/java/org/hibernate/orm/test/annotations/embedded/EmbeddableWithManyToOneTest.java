/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embedded;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EmbeddableWithManyToOneTest.EntityTest.class,
				EmbeddableWithManyToOneTest.EntityTest2.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
public class EmbeddableWithManyToOneTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityTest entity = new EntityTest( 1 );
					EntityTest2 entityTest2 = new EntityTest2( 2 );

					EmbeddableTest embeddable = new EmbeddableTest();
					embeddable.setEntity( entity );
					embeddable.setStringField( "Fab" );

					entityTest2.setEmbeddedAttribute( embeddable );

					entity.setEntity2( entityTest2 );
					session.persist( entity );
					session.persist( entityTest2 );
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
					EntityTest entity = session.get( EntityTest.class, 1 );
					EntityTest2 entity2 = entity.getEntity2();
					assertSame( entity2.getEmbeddedAttribute().getEntity(), entity );
					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 2 );
				}
		);
	}

	@Test
	public void testGet2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityTest entity = new EntityTest( 3 );
					EntityTest2 entityTest2 = new EntityTest2( 4 );

					EntityTest entity3 = new EntityTest( 5 );

					EmbeddableTest embeddable = new EmbeddableTest();
					embeddable.setEntity( entity3 );
					embeddable.setStringField( "Fab" );

					entityTest2.setEmbeddedAttribute( embeddable );

					entity.setEntity2( entityTest2 );
					session.persist( entity );
					session.persist( entity3 );
					session.persist( entityTest2 );
				}
		);
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					EntityTest entity = session.get( EntityTest.class, 3 );
					EntityTest2 entity2 = entity.getEntity2();
					EntityTest entity3 = entity2.getEmbeddedAttribute().getEntity();
					assertThat( entity3.getId(), is( 5 ) );
					assertThat( entity3.getEntity2(), nullValue() );
					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 2 );
				}
		);
	}

	@Test
	public void testGet3(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityTest entity = new EntityTest( 3 );
					EntityTest2 entityTest = new EntityTest2( 4 );

					EntityTest entity3 = new EntityTest( 5 );

					EntityTest2 entityTest2 = new EntityTest2( 6 );

					entity3.setEntity2( entityTest2 );
					EmbeddableTest embeddable = new EmbeddableTest();
					embeddable.setEntity( entity3 );
					embeddable.setStringField( "Fab" );

					entityTest.setEmbeddedAttribute( embeddable );

					entity.setEntity2( entityTest );
					session.persist( entity );
					session.persist( entity3 );
					session.persist( entityTest );
					session.persist( entityTest2 );
				}
		);
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					EntityTest entity = session.get( EntityTest.class, 3 );
					EntityTest2 entity2 = entity.getEntity2();
					EntityTest entity3 = entity2.getEmbeddedAttribute().getEntity();
					assertThat( entity3.getId(), is( 5 ) );
					assertThat( entity3.getEntity2().getId(), is( 6 ) );
					statementInspector.assertExecutedCount( 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 2 );
				}
		);
	}

	@Entity(name = "EntityTest")
	public static class EntityTest {
		@Id
		private Integer id;

		private String description;

		@ManyToOne
		private EntityTest2 entity2;

		public EntityTest() {
		}

		public EntityTest(int id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityTest2 getEntity2() {
			return entity2;
		}

		public void setEntity2(EntityTest2 entity2) {
			this.entity2 = entity2;
		}
	}

	@Entity(name = "EntityTest2")
	public static class EntityTest2 {
		@Id
		private Integer id;

		@Embedded
		private EmbeddableTest embeddedAttribute;

		public EntityTest2() {
		}

		public EntityTest2(int id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
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
	public static class EmbeddableTest {
		private String stringField;

		@ManyToOne
		private EntityTest entity;

		public String getStringField() {
			return stringField;
		}

		public void setStringField(String stringField) {
			this.stringField = stringField;
		}

		public EntityTest getEntity() {
			return entity;
		}

		public void setEntity(EntityTest entity) {
			this.entity = entity;
		}
	}
}
