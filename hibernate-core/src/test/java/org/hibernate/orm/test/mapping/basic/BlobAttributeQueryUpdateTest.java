/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;

import org.hibernate.LobHelper;
import org.hibernate.engine.jdbc.proxy.BlobProxy;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.TypedQuery;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				BlobAttributeQueryUpdateTest.TestEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-18480")
public class BlobAttributeQueryUpdateTest {

	private static final byte[] INITIAL_BYTES = "initial".getBytes( StandardCharsets.UTF_8 );
	private static final byte[] UPDATED_BYTES_1 = "update1".getBytes( StandardCharsets.UTF_8 );
	private static final byte[] UPDATED_BYTES_2 = "update2".getBytes( StandardCharsets.UTF_8 );

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					LobHelper lobHelper = session.getLobHelper();
					TestEntity testEntity = new TestEntity(
							1,
							"test",
							lobHelper.createBlob( INITIAL_BYTES )
					);
					session.persist( testEntity );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testUpdateUsingBlobProxy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			TestEntity testEntity = session.find( TestEntity.class, 1 );
			Blob blobValue1 = BlobProxy.generateProxy( UPDATED_BYTES_1 );
			testEntity.setBlobValue( blobValue1 );
		} );

		scope.inTransaction( session -> {
			TestEntity testEntity = session.find( TestEntity.class, 1 );
			checkBlobValue( testEntity, UPDATED_BYTES_1 );
		} );

		scope.inTransaction(
				session -> {
					TypedQuery<?> query = session.createQuery(
							"UPDATE TestEntity b SET b.blobValue = :blobValue WHERE b.id = :id",
							null
					);
					query.setParameter( "id", 1 );
					Blob value = BlobProxy.generateProxy( UPDATED_BYTES_2 );
					query.setParameter( "blobValue", value );
					query.executeUpdate();
				}
		);

		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, 1 );
					checkBlobValue( testEntity, UPDATED_BYTES_2 );
				}
		);
	}

	@Test
	public void testUpdateUsingLobHelper(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			LobHelper lobHelper = session.getLobHelper();
			TestEntity testEntity = session.find( TestEntity.class, 1 );
			Blob blobValue1 = lobHelper.createBlob( UPDATED_BYTES_1 );
			testEntity.setBlobValue( blobValue1 );
		} );

		scope.inTransaction( session -> {
			TestEntity testEntity = session.find( TestEntity.class, 1 );
			checkBlobValue( testEntity, UPDATED_BYTES_1 );
		} );


		scope.inTransaction(
				session -> {
					LobHelper lobHelper = session.getLobHelper();
					TypedQuery<?> query = session.createQuery(
							"UPDATE TestEntity b SET b.blobValue = :blobValue WHERE b.id = :id",
							null
					);
					query.setParameter( "id", 1 );
					Blob value = lobHelper.createBlob( UPDATED_BYTES_2 );
					query.setParameter( "blobValue", value );
					query.executeUpdate();
				}
		);

		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, 1 );
					checkBlobValue( testEntity, UPDATED_BYTES_2 );
				}
		);
	}

	private static void checkBlobValue(TestEntity testEntity, byte[] expectedValue) {
		try {
			assertThat( new String( testEntity.getBlobValue()
											.getBytes( 1, (int) testEntity.getBlobValue().length() ) ) )
					.isEqualTo( new String( expectedValue ) );
		}
		catch (SQLException e) {
			throw new RuntimeException( e );
		}
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Integer id;

		private String name;

		private Blob blobValue;

		public TestEntity() {
		}

		public TestEntity(Integer id, String name, Blob blobValue) {
			this.id = id;
			this.name = name;
			this.blobValue = blobValue;
		}

		public Integer getId() {
			return id;
		}

		public Blob getBlobValue() {
			return blobValue;
		}

		public void setBlobValue(Blob blobValue) {
			this.blobValue = blobValue;
		}
	}
}
