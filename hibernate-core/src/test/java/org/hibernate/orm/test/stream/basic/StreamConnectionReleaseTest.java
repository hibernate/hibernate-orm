/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stream.basic;

import java.sql.Blob;
import java.sql.SQLException;
import java.util.stream.Stream;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.descriptor.java.BlobJavaType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@DomainModel(
		annotatedClasses = {StreamConnectionReleaseTest.MyEntity.class, StreamConnectionReleaseTest.MyBlobEntity.class})
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

				MyBlobEntity eb = new MyBlobEntity();
				eb.id = i;
				eb.name = "Entity #" + i;
				eb.blob = BlobJavaType.INSTANCE.fromEncodedString( "aaaaaaaaaaaaaaaaaa" );
				session.persist( eb );
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
	void testConnectionAndBlobsFreed(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertThat( session.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected() )
					.as( "connection should not be acquired before the query" )
					.isFalse();

			try (Stream<MyBlobEntity> stream = session.createQuery( "from MyBlobEntity", MyBlobEntity.class )
					.getResultStream()) {
				stream.forEach( e -> {
					assertThat( e.name ).isNotNull();
					try {
						assertThat( e.blob.length() ).isPositive();
						e.blob.free();
					}
					catch (SQLException ex) {
						fail( ex );
					}
				} );
			}

			assertThat( session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources() )
					.as( "resources should be released after stream close" )
					.isFalse();

			assertThat( session.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected() )
					.as( "connection should be released after stream close outside a transaction" )
					.isFalse();
		} );
	}

	@Test
	void testConnectionAndBlobsDontCallFreeInSessionExplicitly(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertThat( session.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected() )
					.as( "connection should not be acquired before the query" )
					.isFalse();

			try (Stream<MyBlobEntity> stream = session.createQuery( "from MyBlobEntity", MyBlobEntity.class )
					.getResultStream()) {
				stream.forEach( e -> {
					assertThat( e.name ).isNotNull();
					try {
						assertThat( e.blob.length() ).isPositive();
					}
					catch (SQLException ex) {
						fail( ex );
					}
				} );
			}

			if ( scope.getSessionFactory().getJdbcServices()
					.getDialect().supportsUnboundedLobLocatorMaterialization() ) {
				// LOBs are not registered with the resource registry when the dialect
				// supports unbounded LOB locator materialization
				assertThat( session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources() )
						.as( "no LOB resources should be registered" )
						.isFalse();
				assertThat( session.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected() )
						.as( "connection should be released after stream close outside a transaction" )
						.isFalse();
			}
			else {
				// because we are not explicitly calling blob.free() all the blobs remain registered.
				assertThat( session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources() )
						.as( "LOB resources should still be registered" )
						.isTrue();
				assertThat( session.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected() )
						.as( "connection should remain open while LOB resources are registered" )
						.isTrue();
			}
		} );
	}

	@Entity(name = "MyEntity")
	public static class MyEntity {
		@Id
		public Integer id;
		public String name;
	}

	@Entity(name = "MyBlobEntity")
	public static class MyBlobEntity {
		@Id
		public Integer id;
		@Lob
		private Blob blob;
		public String name;
	}
}
