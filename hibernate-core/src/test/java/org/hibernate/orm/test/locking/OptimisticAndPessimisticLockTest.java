/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.hibernate.LockMode;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

@DomainModel(annotatedClasses = {
		OptimisticAndPessimisticLockTest.EntityA.class
})
@SessionFactory
@JiraKey("HHH-16461")
@SkipForDialect(dialectClass = CockroachDialect.class, reason = "CockroachDB uses SERIALIZABLE isolation, and does not support this")
@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 11, minorVersion = 6, microVersion = 2, reason = "MariaDB will throw an error DB_RECORD_CHANGED when acquiring a lock on a record that have changed")
public class OptimisticAndPessimisticLockTest {

	public Stream<LockMode> pessimisticLockModes() {
		return Stream.of(LockMode.UPGRADE_NOWAIT, LockMode.PESSIMISTIC_WRITE, LockMode.PESSIMISTIC_READ, LockMode.PESSIMISTIC_FORCE_INCREMENT);
	}

	@ParameterizedTest
	@MethodSource(value = "pessimisticLockModes")
	public void upgradeFromOptimisticToPessimisticLock(LockMode pessimisticLockMode, SessionFactoryScope scope) {
		Integer id = scope.fromTransaction( session -> {
			EntityA entityA1 = new EntityA();
			entityA1.setPropertyA( 1 );
			session.persist( entityA1 );
			return entityA1.getId();
		} );
		scope.inTransaction( session -> {
			EntityA entityA1 = session.find( EntityA.class, id );

			// Do a concurrent change that will update the @Version property
			scope.inTransaction( session2 -> {
				var concurrentEntityA1 = session2.find( EntityA.class, id );
				concurrentEntityA1.setPropertyA( concurrentEntityA1.getPropertyA() + 1 );
			} );

			// Refresh the entity with concurrent changes and upgrade the lock
			session.refresh( entityA1, pessimisticLockMode );

			entityA1.setPropertyA( entityA1.getPropertyA() * 2 );
		} );
		scope.inTransaction( session -> {
			EntityA entityA1 = session.find( EntityA.class, id );
			assertThat( entityA1.getPropertyA() ).isEqualTo( ( 1 + 1 ) * 2 );
		} );
	}

	@Entity(name = "EntityA")
	public static class EntityA {

		@Id
		@GeneratedValue
		Integer id;

		@Version
		long version;

		int propertyA;

		public EntityA() {
		}

		public EntityA(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public long getVersion() {
			return version;
		}

		public int getPropertyA() {
			return propertyA;
		}

		public void setPropertyA(int propertyA) {
			this.propertyA = propertyA;
		}
	}
}
