/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		EmbeddedForeignKeyInSubqueryTest.EnvironmentKey.class,
		EmbeddedForeignKeyInSubqueryTest.EnvironmentEntity.class,
		EmbeddedForeignKeyInSubqueryTest.OperationEntity.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17280" )
public class EmbeddedForeignKeyInSubqueryTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EnvironmentEntity environment1 = new EnvironmentEntity( new EnvironmentKey( "env_1", 1 ) );
			session.persist( environment1 );
			session.persist( new OperationEntity( 1L, OperationStatus.PENDING, environment1 ) );
			final EnvironmentEntity environment2 = new EnvironmentEntity( new EnvironmentKey( "env_2", 2 ) );
			session.persist( environment2 );
			session.persist( new OperationEntity( 2L, OperationStatus.IN_PROGRESS, environment2 ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from OperationEntity" ).executeUpdate();
			session.createMutationQuery( "delete from EnvironmentEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testImplicitJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EnvironmentEntity result = session.createQuery(
					"select o.environment from OperationEntity o where o.environment.key not in" +
							" (select o2.environment.key from OperationEntity o2 where o2.status in ('IN_PROGRESS', 'ERROR'))",
					EnvironmentEntity.class
			).getSingleResult();
			assertThat( result.getKey().getName() ).isEqualTo( "env_1" );
			assertThat( result.getKey().getCode() ).isEqualTo( 1 );
		} );
	}

	@Test
	public void testExplicitJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EnvironmentEntity result = session.createQuery(
					"select e from OperationEntity o join o.environment e where e.key not in" +
							" (select e2.key from OperationEntity o2 join o2.environment e2 where o2.status in ('IN_PROGRESS', 'ERROR'))",
					EnvironmentEntity.class
			).getSingleResult();
			assertThat( result.getKey().getName() ).isEqualTo( "env_1" );
			assertThat( result.getKey().getCode() ).isEqualTo( 1 );
		} );
	}

	@Embeddable
	public static class EnvironmentKey implements Serializable {
		private String name;

		private Integer code;

		public EnvironmentKey() {
		}

		public EnvironmentKey(String name, Integer code) {
			this.name = name;
			this.code = code;
		}

		public String getName() {
			return name;
		}

		public Integer getCode() {
			return code;
		}
	}


	@Entity( name = "EnvironmentEntity" )
	public static class EnvironmentEntity {
		@EmbeddedId
		private EnvironmentKey key;

		public EnvironmentEntity() {
		}

		public EnvironmentEntity(EnvironmentKey key) {
			this.key = key;
		}

		public EnvironmentKey getKey() {
			return key;
		}
	}

	public enum OperationStatus {
		ERROR,
		IN_PROGRESS,
		PENDING,
		SUCCESS
	}

	@Entity( name = "OperationEntity" )
	public static class OperationEntity {
		@Id
		private Long id;

		@Enumerated( EnumType.STRING )
		private OperationStatus status;

		@ManyToOne
		private EnvironmentEntity environment;

		public OperationEntity() {
		}

		public OperationEntity(Long id, OperationStatus status, EnvironmentEntity environment) {
			this.id = id;
			this.status = status;
			this.environment = environment;
		}

		public OperationStatus getStatus() {
			return status;
		}

		public EnvironmentEntity getEnvironment() {
			return environment;
		}
	}
}
