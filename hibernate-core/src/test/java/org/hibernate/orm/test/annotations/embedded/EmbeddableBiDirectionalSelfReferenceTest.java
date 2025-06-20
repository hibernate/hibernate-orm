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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hibernate.orm.test.annotations.embedded.EmbeddableBiDirectionalSelfReferenceTest.EntityTest;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EntityTest.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
public class EmbeddableBiDirectionalSelfReferenceTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					EntityTest entity = new EntityTest( 1 );

					EmbeddableTest embeddable = new EmbeddableTest();
					embeddable.setEntity( entity );
					embeddable.setStringField( "Fab" );
					entity.setEmbeddedAttribute( embeddable );

					session.persist( entity );
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
					EmbeddableTest embeddedAttribute = entity.getEmbeddedAttribute();
					assertThat( embeddedAttribute, notNullValue() );
					assertThat( embeddedAttribute.getStringField(), is( "Fab" ) );
					assertSame( entity, embeddedAttribute.getEntity() );
					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );
				}
		);
	}

	@Test
	public void testGet2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityTest entity = new EntityTest( 2 );

					EntityTest entity2 = new EntityTest( 3 );

					EntityTest entity3 = new EntityTest( 4 );

					EmbeddableTest embeddable = new EmbeddableTest();
					embeddable.setEntity( entity2 );
					embeddable.setStringField( "Acme" );
					entity.setEmbeddedAttribute( embeddable );

					EmbeddableTest embeddable2 = new EmbeddableTest();
					embeddable2.setEntity( entity3 );
					embeddable2.setStringField( "Acme2" );
					entity2.setEmbeddedAttribute( embeddable2 );

					session.persist( entity );
					session.persist( entity2 );
					session.persist( entity3 );
				}
		);

		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					EntityTest entity = session.get( EntityTest.class, 2 );
					EmbeddableTest embeddedAttribute = entity.getEmbeddedAttribute();
					assertThat( embeddedAttribute, notNullValue() );
					assertThat( embeddedAttribute.getStringField(), is( "Acme" ) );

					EntityTest entity2 = embeddedAttribute.getEntity();
					assertThat( entity2, notNullValue() );
					assertThat( entity2.getId(), is( 3 ) );

					EmbeddableTest embeddedAttribute2 = entity2.getEmbeddedAttribute();
					assertThat( embeddedAttribute2.getStringField(), is( "Acme2" ) );

					EntityTest entity3 = embeddedAttribute2.getEntity();
					assertThat( entity3.getId(), is( 4 ) );
					assertThat( entity3.getEmbeddedAttribute(), nullValue() );

					statementInspector.assertExecutedCount( 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 1 );
				}
		);
	}

	@Entity(name = "EntityTest")
	public static class EntityTest {
		@Id
		private Integer id;

		@Embedded
		private EmbeddableTest embeddedAttribute;

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
