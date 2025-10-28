/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lob;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import org.hibernate.engine.jdbc.env.internal.NonContextualLobCreator;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = InflaterInputStreamBlobTest.TestEntity.class)
@SessionFactory
@JiraKey("HHH-19464")
class InflaterInputStreamBlobTest {
	private static final int RANDOM_SIZE = 32000;

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void hibernate_blob_streaming(SessionFactoryScope scope) throws Exception {
		final var randomBytes = getRandomBytes();
		final var outputStream = new ByteArrayOutputStream();
		try (var zipOutputStream = new GZIPOutputStream( outputStream )) {
			zipOutputStream.write( randomBytes );
		}

		long size = randomBytes.length;
		scope.inTransaction( entityManager -> {
					try {
						InputStream is = new GZIPInputStream( new ByteArrayInputStream( outputStream.toByteArray() ) );
						Blob blob = NonContextualLobCreator.INSTANCE.wrap(
								NonContextualLobCreator.INSTANCE.createBlob( is, size )
						);
						TestEntity e = new TestEntity();
						e.setId( 1L );
						e.setData( blob );

						entityManager.persist( e );
					}
					catch (IOException e) {
						throw new RuntimeException( e );
					}
				}
		);

		scope.inStatelessSession( session -> {
			final var entity = session.get( TestEntity.class, 1L );
			try {
				final var blob = entity.getData();
				assertEquals( size, blob.length() );
				assertArrayEquals( randomBytes, blob.getBytes( 1L, (int) blob.length() ) );
			}
			catch (SQLException e) {
				throw new RuntimeException( e );
			}
		} );
	}

	private static byte[] getRandomBytes() {
		final var bytes = new byte[RANDOM_SIZE];
		new Random().nextBytes( bytes );
		return bytes;
	}

	@Entity
	public static class TestEntity {

		@Id
		Long id;

		@Lob
		Blob data;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Blob getData() {
			return data;
		}

		public InputStream getInputStream() {
			try {
				return data.getBinaryStream();
			}
			catch (SQLException e) {
				throw new IllegalArgumentException( "Could not obtain requested input stream", e );
			}
		}

		public void setData(Blob data) {
			this.data = data;
		}
	}
}
