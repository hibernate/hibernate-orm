/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stream.basic;

import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {StreamConnectionReleaseTest.MyEntity.class})
@SessionFactory
@ServiceRegistry(settings = {
		// We want to make sure that the release mode is set to "after transaction"
		// but at the same time we are running things without an actual transaction.
		// Resources should be still released in this case ...
		@Setting(name = AvailableSettings.CONNECTION_HANDLING,
				value = "DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION")
})
public class StreamConnectionReleaseTest {

	@BeforeEach
	void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int i = 0; i < 5; i++ ) {
				MyEntity e = new MyEntity();
				e.id = i;
				e.name = "Entity #" + i;
				session.persist( e );
			}
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testConnectionReleasedAfterStreamCloseOutsideTransaction(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertThat( session.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected() )
					.as( "connection should not be acquired before the query" )
					.isFalse();

			try (Stream<MyEntity> stream = session.createQuery( "from MyEntity", MyEntity.class ).getResultStream()) {
				stream.forEach( e -> assertThat( e.name ).isNotNull() );
				assertThat( session.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected() )
						.as( "we are still holding the connection because the stream is opened" )
						.isTrue();
			}

			assertThat(
					session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources() )
					.as( "resources should be released after stream close" )
					.isFalse();

			assertThat( session.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected() )
					.as( "connection should be released after stream close outside a transaction" )
					.isFalse();
		} );
	}

	@Test
	void testConnectionReleasedAfterStatementOutsideTransaction(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertThat( session.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected() )
					.as( "connection should not be acquired before the query" )
					.isFalse();

			List<MyEntity> list = session.createQuery( "from MyEntity", MyEntity.class ).getResultList();
			list.forEach( e -> assertThat( e.name ).isNotNull() );

			assertThat(
					session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry()
							.hasRegisteredResources() )
					.as( "no resources were requested by this query" )
					.isFalse();

			assertThat( session.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected() )
					.as( "connection should be released after the statement" )
					.isFalse();
		} );
	}

	@Entity(name = "MyEntity")
	public static class MyEntity {
		@Id
		public Integer id;
		public String name;
	}
}
