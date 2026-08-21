/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;
import org.hibernate.annotations.PartitionKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@DomainModel(
		annotatedClasses = {
				PartitionKeyAndVersionTest.TestEntity.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-20287")
public class PartitionKeyAndVersionTest {

	private static final Long ENTITY_ID = 1L;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TestEntity( ENTITY_ID, 1 ) );
		} );
	}

	@Test
	public void testSetOnlyTheVersion(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			TestEntity testEntity = session.find( TestEntity.class, ENTITY_ID );
			testEntity.setVersion( testEntity.getVersion() + 1 );
		} );
	}

	@Test
	public void testForceIncrement(SessionFactoryScope scope) {
		scope.inTransaction( session ->
				session.find( TestEntity.class, ENTITY_ID, LockModeType.OPTIMISTIC_FORCE_INCREMENT )
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		public Long id;

		@PartitionKey
		public int partitionKey;

		@Version
		public int version;

		public String name;

		public TestEntity() {
		}

		public TestEntity(Long id, int partitionKey) {
			this.id = id;
			this.partitionKey = partitionKey;
		}

		public Long getId() {
			return id;
		}

		public int getPartitionKey() {
			return partitionKey;
		}

		public int getVersion() {
			return version;
		}

		public void setVersion(int version) {
			this.version = version;
		}
	}
}
