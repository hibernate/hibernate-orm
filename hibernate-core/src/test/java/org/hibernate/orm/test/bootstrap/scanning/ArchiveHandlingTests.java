/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.scanning;

import org.hibernate.boot.archive.internal.ExplodedArchiveDescriptor;
import org.hibernate.boot.archive.internal.JarFileBasedArchiveDescriptor;
import org.hibernate.boot.archive.internal.JarInputStreamBasedArchiveDescriptor;
import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;
import org.hibernate.boot.jaxb.internal.ConfigurationBinder;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.orm.test.boot.archive.OneEntity;
import org.hibernate.orm.test.boot.archive.SecondEntity;
import org.hibernate.orm.test.boot.archive.ThirdEntity;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/// Unit tests for impls of `ArchiveDescriptor`
///
/// @author Steve Ebersole
@ServiceRegistry
public class ArchiveHandlingTests {
	public static final StandardArchiveDescriptorFactory ARCHIVE_HANDLER = StandardArchiveDescriptorFactory.INSTANCE;

	public static final String EXTERNAL_JAR_NAME = "external.jar";
	public static final String EXTERNAL_JAR_REF = "META-INF/" + EXTERNAL_JAR_NAME;

	@Test
	void testExplodedArchiveHandling(@TempDir File stagingDirectory, ServiceRegistryScope registryScope) throws MalformedURLException {
		var externalJarFile = buildExternalJarFile( stagingDirectory );

		var archiveName = "exploded-archive";
		var primaryArchive = buildPrimaryArchive( archiveName, externalJarFile );
		primaryArchive.as( ExplodedExporter.class ).exportExploded( stagingDirectory );
		var primaryArchiveFile = new File( stagingDirectory, archiveName );
		primaryArchiveFile.mkdirs();

		var primaryArchiveDescriptor = new ExplodedArchiveDescriptor( ARCHIVE_HANDLER, primaryArchiveFile.toURI().toURL(), null );

		checkPrimaryArchive( primaryArchiveDescriptor, registryScope.getRegistry() );
	}

	@Test
	void testJarFileHandling(@TempDir File stagingDirectory, ServiceRegistryScope registryScope) throws MalformedURLException {
		var externalJarFile = buildExternalJarFile( stagingDirectory );

		var archiveName = "root.jar";
		var primaryArchive = buildPrimaryArchive( archiveName, externalJarFile );
		var primaryArchiveFile = new File( stagingDirectory, archiveName );
		primaryArchive.as( ZipExporter.class ).exportTo( primaryArchiveFile );

		var jarFileUrl = primaryArchiveFile.toURI().toURL();
		assertThat( jarFileUrl.getProtocol() ).isEqualTo( "file" );

		var primaryArchiveDescriptor = new JarFileBasedArchiveDescriptor( ARCHIVE_HANDLER, jarFileUrl, null );

		checkPrimaryArchive( primaryArchiveDescriptor, registryScope.getRegistry() );
	}

	@Test
	void testJarInputStreamHandling(@TempDir File stagingDirectory, ServiceRegistryScope registryScope) throws MalformedURLException {
		var externalJarFile = buildExternalJarFile( stagingDirectory );

		var archiveName = "root.jar";
		var primaryArchive = buildPrimaryArchive( archiveName, externalJarFile );
		var primaryArchiveFile = new File( stagingDirectory, archiveName );
		primaryArchive.as( ZipExporter.class ).exportTo( primaryArchiveFile );

		var jarFileUrl = primaryArchiveFile.toURI().toURL();
		assertThat( jarFileUrl.getProtocol() ).isEqualTo( "file" );

		var primaryArchiveDescriptor = new JarInputStreamBasedArchiveDescriptor( ARCHIVE_HANDLER, jarFileUrl, null );

		checkPrimaryArchive( primaryArchiveDescriptor, registryScope.getRegistry() );
	}

	public static Archive<?> buildPrimaryArchive(String archiveName, File externalJarFile) {
		var archive = ShrinkWrap.create( JavaArchive.class, archiveName );

		archive.addAsResource( "units/pack/persistence.xml", ArchivePaths.create( "META-INF/persistence.xml" ) );

		archive.addAsResource( externalJarFile, ArchivePaths.create( EXTERNAL_JAR_REF ) );

		return archive;
	}

	public static File buildExternalJarFile(File stagingDirectory) {
		var archive = ShrinkWrap.create( JavaArchive.class, EXTERNAL_JAR_NAME );
		archive.addClasses( OneEntity.class );
		archive.addClasses( SecondEntity.class );
		archive.addClasses( ThirdEntity.class );

		archive.addAsResource( "units/pack/nested-orm.xml", ArchivePaths.create( "META-INF/orm.xml" ) );

		var jarFile = new File( stagingDirectory, EXTERNAL_JAR_NAME );
		archive.as( ZipExporter.class ).exportTo( jarFile, true );
		return jarFile;
	}



	private void checkPrimaryArchive(ArchiveDescriptor primaryArchiveDescriptor, StandardServiceRegistry registry) throws MalformedURLException {
		final ArchiveEntry persistenceXmlEntry = primaryArchiveDescriptor.findEntry( "META-INF/persistence.xml" );
		checkPersistenceXml( persistenceXmlEntry, registry );

		final ArchiveDescriptor externalJar = primaryArchiveDescriptor.resolveJarFileReference( EXTERNAL_JAR_REF );
		checkExternalJarEntry( externalJar, primaryArchiveDescriptor, registry );
	}

	private void checkPersistenceXml(ArchiveEntry persistenceXmlEntry, StandardServiceRegistry registry) {
		assertThat( persistenceXmlEntry ).isNotNull();

		assertThat( persistenceXmlEntry.getNameWithinArchive() ).isEqualTo( "META-INF/persistence.xml" );
		assertThat( persistenceXmlEntry.getUri() ).isNotNull();
		assertThat( persistenceXmlEntry.getUri().toString() ).endsWith(  "META-INF/persistence.xml" );

		var xmlBinder = new ConfigurationBinder(null);
		var origin = new Origin( SourceType.INPUT_STREAM, "META-INF/persistence.xml" );

		try (var stream = persistenceXmlEntry.getStreamAccess().accessInputStream()) {
			final JaxbPersistenceImpl jaxbPersistence = xmlBinder.bind( stream, origin ).getRoot();
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	private void checkExternalJarEntry(ArchiveDescriptor nestedArchiveDescriptor, ArchiveDescriptor primaryArchiveDescriptor, StandardServiceRegistry registry)
			throws MalformedURLException {
		assertThat( nestedArchiveDescriptor ).isNotNull();

		List<ArchiveEntry> entries = new ArrayList<>();
		nestedArchiveDescriptor.visitClassEntries( entries::add );
		assertThat( entries ).hasSize( 3 );

		final ArchiveEntry ormXmlEntry = nestedArchiveDescriptor.findEntry( "META-INF/orm.xml" );
		checkOrmXml( ormXmlEntry, registry );
	}

	private void checkOrmXml(ArchiveEntry ormXmlEntry, StandardServiceRegistry registry) {
		assertThat( ormXmlEntry ).isNotNull();

		assertThat( ormXmlEntry.getNameWithinArchive() ).isEqualTo( "META-INF/orm.xml" );
		assertThat( ormXmlEntry.getUri() ).isNotNull();
		assertThat( ormXmlEntry.getUri().toString() ).endsWith(  "META-INF/orm.xml" );

		final MappingBinder xmlBinder = new MappingBinder( registry );
		var origin = new Origin( SourceType.INPUT_STREAM, "META-INF/orm.xml" );

		try (var stream = ormXmlEntry.getStreamAccess().accessInputStream()) {
			final JaxbBindableMappingDescriptor root = xmlBinder.bind( stream, origin ).getRoot();
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}
}
