/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.version;

import java.util.UUID;

import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;

@DomainModel(
		annotatedClasses = {
				VersionedBidirectionalOneToOneMergeTest.TestEntity.class,
				VersionedBidirectionalOneToOneMergeTest.AnotherTestEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-16833")
public class VersionedBidirectionalOneToOneMergeTest {

	@Test
	public void testMerge(SessionFactoryScope scope) {
		AnotherTestEntity anotherTestEntity = new AnotherTestEntity();
		scope.inTransaction(
				session -> {
					session.merge( anotherTestEntity );
				}
		);

		scope.inTransaction(
				session -> {
					TestEntity testEntity = new TestEntity( anotherTestEntity );
					session.persist( testEntity );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		UUID uuid = SafeRandomUUIDGenerator.safeRandomUUID();

		@Version
		Long version;

		@OneToOne
		@JoinColumn(name = "OTHER_ENTITY_ID")
		AnotherTestEntity anotherTestEntity;

		public TestEntity() {
		}

		public TestEntity(AnotherTestEntity anotherTestEntity) {
			this.anotherTestEntity = anotherTestEntity;
		}
	}

	@Entity(name = "AnotherTestEntity")
	public static class AnotherTestEntity {
		@Id
		UUID uuid = SafeRandomUUIDGenerator.safeRandomUUID();

		String name;

		@OneToOne(mappedBy = "anotherTestEntity")
		TestEntity testEntity;

	}
}
