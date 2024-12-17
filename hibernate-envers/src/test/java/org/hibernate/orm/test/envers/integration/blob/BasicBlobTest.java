/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.blob;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hamcrest.Matchers;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.jdbc.BlobProxy;
import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.testing.SkipForDialect;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Blob;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.fail;

/**
 * @author Chris Cranford
 */
public class BasicBlobTest extends BaseEnversJPAFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Asset.class};
	}

	@Test
	@Priority(10)
	public void testGenerateProxyNoStream() {
		final Path path = Path.of( Thread.currentThread().getContextClassLoader()
				.getResource( "org/hibernate/orm/test/envers/integration/blob/blob.txt" ).getPath() );
		doInJPA( this::entityManagerFactory, entityManager -> {
			final Asset asset = new Asset();
			asset.setFileName( "blob.txt" );

			try (final InputStream stream = new BufferedInputStream( Files.newInputStream( path ) )) {
				assertThat( stream.markSupported(), Matchers.is( true ) );

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
	@Priority(10)
	@SkipForDialect(value = PostgreSQLDialect.class,
			comment = "The driver closes the stream, so it cannot be reused by envers")
	@SkipForDialect(value = SQLServerDialect.class,
			comment = "The driver closes the stream, so it cannot be reused by envers")
	@SkipForDialect(value = SybaseDialect.class,
			comment = "The driver closes the stream, so it cannot be reused by envers")
	public void testGenerateProxyStream() {
		final Path path = Path.of( Thread.currentThread().getContextClassLoader()
				.getResource( "org/hibernate/orm/test/envers/integration/blob/blob.txt" ).getPath() );

		try (final InputStream stream = new BufferedInputStream( Files.newInputStream( path ) )) {
			doInJPA( this::entityManagerFactory, entityManager -> {
				final Asset asset = new Asset();
				asset.setFileName( "blob.txt" );

				assertThat( stream.markSupported(), Matchers.is( true ) );

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
				Blob blob = BlobProxy.generateProxy( stream, 1431 );

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
