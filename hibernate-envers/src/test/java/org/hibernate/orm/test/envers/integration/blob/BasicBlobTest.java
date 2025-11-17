/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.blob;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.proxy.BlobProxy;
import org.hibernate.envers.Audited;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.envers.junit.EnversTest;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Blob;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Chris Cranford
 */
@EnversTest
@Jpa(annotatedClasses = {BasicBlobTest.Asset.class})
public class BasicBlobTest {

	@BeforeClassTemplate
	public void testGenerateProxyNoStream(EntityManagerFactoryScope scope) throws URISyntaxException {
		final Path path = Path.of( Thread.currentThread().getContextClassLoader()
				.getResource( "org/hibernate/orm/test/envers/integration/blob/blob.txt" ).toURI() );
		scope.inTransaction( entityManager -> {
			final Asset asset = new Asset();
			asset.setFileName( "blob.txt" );

			try (final InputStream stream = new BufferedInputStream( Files.newInputStream( path ) )) {
				assertThat( stream.markSupported(), is( true ) );

				// We use the method readAllBytes instead of passing the raw stream to the proxy
				// since this is the only guaranteed way that will work across all dialects in a
				// deterministic way.  Postgres and Sybase will automatically close the stream
				// after the blob has been written by the driver, which prevents Envers from
				// then writing the contents of the stream to the audit table.
				//
				// If the driver and dialect are known not to close the input stream after the
				// contents have been written by the driver, then it's safe to pass the stream
				// here instead and the stream will be automatically marked and reset so that
				// Envers can serialize the data after Hibernate has done so.  Dialects like
				// H2, MySQL, Oracle, SQL Server work this way.
				//
				//
				Blob blob = BlobProxy.generateProxy( stream.readAllBytes() );

				asset.setData( blob );
				entityManager.persist( asset );
			}
			catch (Exception e) {
				e.printStackTrace();
				fail( "Failed to persist the entity" );
			}
		} );
	}

	@Test
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, matchSubTypes = true,
			reason = "The driver closes the stream, so it cannot be reused by envers")
	@SkipForDialect(dialectClass = SQLServerDialect.class, matchSubTypes = true,
			reason = "The driver closes the stream, so it cannot be reused by envers")
	@SkipForDialect(dialectClass = InformixDialect.class, matchSubTypes = true)
	public void testGenerateProxyStream(EntityManagerFactoryScope scope) throws URISyntaxException {
		final Path path = Path.of( Thread.currentThread().getContextClassLoader()
				.getResource( "org/hibernate/orm/test/envers/integration/blob/blob.txt" ).toURI() );

		try (final InputStream stream = new BufferedInputStream( Files.newInputStream( path ) )) {
			final long length = Files.size( path );
			scope.inTransaction( entityManager -> {
				final Asset asset = new Asset();
				asset.setFileName( "blob.txt" );

				assertThat( stream.markSupported(), is( true ) );

				// We use the method readAllBytes instead of passing the raw stream to the proxy
				// since this is the only guaranteed way that will work across all dialects in a
				// deterministic way.  Postgres and Sybase will automatically close the stream
				// after the blob has been written by the driver, which prevents Envers from
				// then writing the contents of the stream to the audit table.
				//
				// If the driver and dialect are known not to close the input stream after the
				// contents have been written by the driver, then it's safe to pass the stream
				// here instead and the stream will be automatically marked and reset so that
				// Envers can serialize the data after Hibernate has done so.  Dialects like
				// H2, MySQL, Oracle, SQL Server work this way.
				//
				//
				Blob blob = BlobProxy.generateProxy( stream, length );

				asset.setData( blob );
				entityManager.persist( asset );
			} );
		}
		catch (Exception e) {
			e.printStackTrace();
			fail( "Failed to persist the entity" );
		}
	}

	@Audited
	@Entity(name = "Asset")
	public static class Asset {
		@Id
		@GeneratedValue
		private Integer id;
		private String fileName;
		private Blob data;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public Blob getData() {
			return data;
		}

		public void setData(Blob data) {
			this.data = data;
		}
	}
}
