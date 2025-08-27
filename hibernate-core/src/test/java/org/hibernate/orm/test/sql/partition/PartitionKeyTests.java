/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.partition;

import org.hibernate.annotations.PartitionKey;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = PartitionKeyTests.PartitionedEntity.class )
@SessionFactory( useCollectingStatementInspector = true )
public class PartitionKeyTests {
	@Test
	public void test(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();

		// update
		scope.inTransaction( (session) -> {
			final PartitionedEntity entity = session.find( PartitionedEntity.class, 1 );
			inspector.clear();
			entity.setName( "The One" );
		} );
		checkWherePredicate( inspector );

		// delete
		scope.inTransaction( (session) -> {
			final PartitionedEntity entity = session.find( PartitionedEntity.class, 1 );
			inspector.clear();
			session.remove( entity );
		} );
		checkWherePredicate( inspector );
	}

	private void checkWherePredicate(SQLStatementInspector inspector) {
		assertThat( inspector.getSqlQueries() ).hasSize( 1 );
		assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "tenant_id=?" );
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new PartitionedEntity( 1, 1, "tbd" ) );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "PartitionedEntity" )
	@Table( name = "entity_table" )
	public static class PartitionedEntity {
		@Id
		private Integer id;
		@PartitionKey
		@Column(name = "tenant_id", updatable = false)
		private Integer tenantId;
		@Basic
		private String name;

		protected PartitionedEntity() {
			// for use by Hibernate
		}

		public PartitionedEntity(Integer id, Integer tenantId, String name) {
			this.id = id;
			this.tenantId = tenantId;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getTenantId() {
			return tenantId;
		}

		public void setTenantId(Integer tenantId) {
			this.tenantId = tenantId;
		}
	}
}
