/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.boot.discovery;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.MalformedURLException;

import static jakarta.persistence.Persistence.ConnectionProperties.JDBC_PASSWORD;
import static jakarta.persistence.Persistence.ConnectionProperties.JDBC_URL;
import static jakarta.persistence.Persistence.ConnectionProperties.JDBC_USER;

/**
 * @author Steve Ebersole
 */
@RequiresDialect(value = H2Dialect.class,
		comment = "Tests explicitly target H2 database by specifying the H2 JDBC URL.")
public class SimpleTests {
	@Test
	void testBaseline() {
		final PersistenceConfiguration cfg = new PersistenceConfiguration( "emf" )
				.property( JDBC_URL, "jdbc:h2:mem:db1" )
				.property( JDBC_USER, "sa" )
				.property( JDBC_PASSWORD, "" )
				.managedClass( Address.class )
				.managedClass( Book.class )
				.managedClass( Library.class );
		try (EntityManagerFactory emf = cfg.createEntityManagerFactory()) {
			assert emf.isOpen();
			emf.getMetamodel().entity( Book.class );
		}
	}

	@Test
	void testSimpleDiscovery(@TempDir File stagingDir) throws MalformedURLException {
		var jarFileName = "simple.jar";

		var archive = ShrinkWrap.create( JavaArchive.class, jarFileName );
		archive.addClass( Address.class );
		archive.addClass( Book.class );
		archive.addClass( Library.class );
		var exportedArchive = new File( stagingDir, jarFileName );
		archive.as( ZipExporter.class ).exportTo( exportedArchive, true );

		final PersistenceConfiguration cfg = new HibernatePersistenceConfiguration( "emf", exportedArchive.toURI().toURL() )
				.property( JDBC_URL, "jdbc:h2:mem:db1" )
				.property( JDBC_USER, "sa" )
				.property( JDBC_PASSWORD, "" );
		try (EntityManagerFactory emf = cfg.createEntityManagerFactory()) {
			assert emf.isOpen();
			emf.getMetamodel().entity( Address.class );
			emf.getMetamodel().entity( Book.class );
			emf.getMetamodel().entity( Library.class );
		}
	}
}
