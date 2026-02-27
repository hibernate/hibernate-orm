/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Test case for HHH-11810
 */
@JiraKey("HHH-11810")
@DomainModel(annotatedClasses = OptimisticForceIncrementNotIncrementedOnFlushTest.MyEntity.class)
@SessionFactory
public class OptimisticForceIncrementNotIncrementedOnFlushTest {

	@Test
	public void hhh11810Test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			MyEntity toPersist = new MyEntity( 0 );
			toPersist.setData( "sample v1" );
			session.persist( toPersist );
		} );

		scope.inTransaction( session -> {
			MyEntity toUpdate = session.find( MyEntity.class, 0L );
			session.lock( toUpdate, LockModeType.OPTIMISTIC_FORCE_INCREMENT );
			toUpdate.setData( "sample v2" );
			session.flush();

			long versionAfterFlush = toUpdate.getVersion();
			assertThat( versionAfterFlush ).isEqualTo( 1L );
		} );

		scope.inTransaction( session -> {
			MyEntity afterUpdate = session.find( MyEntity.class, 0L );
			assertThat( afterUpdate.getVersion() ).isEqualTo( 1L );
		} );
	}

	@Entity(name = "MyEntity")
	public static class MyEntity {

		@Id
		private long id;

		private String data;

		@Version
		private long version;

		public MyEntity(long id) {
			this.id = id;
			version = 0;
		}

		public MyEntity() {
		}

		public long getId() {
			return id;
		}

		public long getVersion() {
			return version;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}

	}
}
