/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Cedomir Igaly
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		JsonEmbeddableAndSchemaNameTest.MyEntity.class,
		JsonEmbeddableAndSchemaNameTest.MyJson.class
} )
@ServiceRegistry( settings = @Setting( name = AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS, value = "true" ) )
@RequiresDialect( PostgreSQLDialect.class )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16612" )
public class JsonEmbeddableAndSchemaNameTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MyJson myJson = new MyJson( "hello", 100L );
			session.persist( new MyEntity( 1L, myJson ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from MyEntity" ).executeUpdate() );
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MyEntity found = session.find( MyEntity.class, 1L );
			found.getJsonProperty().setStringProp( "updated" );
		} );
		scope.inTransaction( session -> {
			final MyEntity found = session.find( MyEntity.class, 1L );
			assertEquals( "updated", found.getJsonProperty().getStringProp() );
		} );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MyEntity result = session.createQuery(
					"from MyEntity e",
					MyEntity.class
			).getSingleResult();
			assertEquals( 100L, result.getJsonProperty().getLongProp() );
		} );
	}

	@Test
	public void testQueryWithFilter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<MyEntity> resultWithFilter = session
					.createQuery( "from MyEntity e where e.jsonProperty.longProp = :x", MyEntity.class )
					.setParameter( "x", 100L )
					.getResultList();
			assertEquals( 1, resultWithFilter.size() );
		} );
	}

	@Embeddable
	public static class MyJson {
		private String stringProp;
		private Long longProp;

		public MyJson() {
		}

		public MyJson(String stringProp, Long longProp) {
			this.stringProp = stringProp;
			this.longProp = longProp;
		}

		public String getStringProp() {
			return stringProp;
		}

		public void setStringProp(String stringProp) {
			this.stringProp = stringProp;
		}

		public Long getLongProp() {
			return longProp;
		}

		public void setLongProp(Long longProp) {
			this.longProp = longProp;
		}
	}

	@Entity( name = "MyEntity" )
	@Table( name = "my_entity", schema = "public" )
	public static class MyEntity {
		@Id
		private Long id;

		@JdbcTypeCode( SqlTypes.JSON )
		private MyJson jsonProperty;

		public MyEntity() {
		}

		public MyEntity(Long id, MyJson jsonProperty) {
			this.id = id;
			this.jsonProperty = jsonProperty;
		}

		public Long getId() {
			return id;
		}

		public MyJson getJsonProperty() {
			return jsonProperty;
		}
	}
}
