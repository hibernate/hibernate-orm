/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.jpa.test.packaging.temp_classloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.metamodel.EntityType;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.test.PersistenceUnitInfoAdapter;
import org.hibernate.jpa.test.packaging.PackagingTestCase;

import org.junit.Test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import pkg.org.hibernate.jpa.test.packaging.temp_classloader.CustomAutoScanner;

import static org.junit.Assert.assertNotNull;

/**
 * @author Janario Oliveira
 */
public class JpaTempClassLoaderCustomScannerTest extends PackagingTestCase {
	@Test
	public void testAutoScan() throws MalformedURLException {
		final File persistencePar = buildPersistencePar();
		File externalJarToScan = buildExternalJarToScan();
		addPackageToClasspath( persistencePar, externalJarToScan );

		final Map<Object, Object> integration = new HashMap<>();
		integration.put( AvailableSettings.SCANNER, CustomAutoScanner.class.getName() );

		EntityManagerFactory emf =
			new HibernatePersistenceProvider().createContainerEntityManagerFactory(
				new PersistenceUnitInfoAdapter() {
					@Override
					public URL getPersistenceUnitRootUrl() {
						try {
							return persistencePar.toURI().toURL();
						}
						catch (MalformedURLException e) {
							throw new IllegalStateException( e );
						}
					}

					@Override
					public ClassLoader getNewTempClassLoader() {
						return new JpaTempClassLoader( getClassLoader() );
					}
				},
				integration
			);


		EntityType<?> entity = emf.getMetamodel().entity( MyEntity.class );
		assertNotNull( entity );

		entity = emf.getMetamodel().entity( AutoEntity.class );
		assertNotNull( entity );

		emf.close();
	}

	private File buildPersistencePar() {
		String fileName = "app-custom-scanner.par";
		JavaArchive archive = ShrinkWrap.create( JavaArchive.class, fileName );
		archive.addClasses( MyEntity.class );

		File testPackage = new File( packageTargetDir, fileName );
		archive.as( ZipExporter.class ).exportTo( testPackage, true );
		return testPackage;
	}

	private File buildExternalJarToScan() {
		String fileName = CustomAutoScanner.AUTO_FOUND_JAR;
		JavaArchive archive = ShrinkWrap.create( JavaArchive.class, fileName );
		archive.addClasses( AutoEntity.class );
		archive.addClasses( CustomAutoScanner.class );

		File testPackage = new File( packageTargetDir, fileName );
		archive.as( ZipExporter.class ).exportTo( testPackage, true );
		return testPackage;
	}

	@Entity
	public static class AutoEntity {
		@Id
		private Integer id;
	}

	@Entity
	public static class MyEntity {
		@Id
		private Integer id;
	}

	public static class JpaTempClassLoader extends ClassLoader {
		private final ClassLoader delegate;

		public JpaTempClassLoader(ClassLoader delegate) {
			this.delegate = delegate;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if ( name.startsWith( "java." ) || name.startsWith( "javax." ) ) {
				return Class.forName(name, resolve, delegate);
			}

			return findClass( name );
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			Class<?> loaded = findLoadedClass( name );
			if ( loaded != null ) {
				return loaded;
			}

			String pathResource = name.replace( '.', '/' ) + ".class";
			try (InputStream input = delegate.getResourceAsStream( pathResource )) {
				if ( input == null ) {
					throw new ClassNotFoundException( name );
				}


				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int nRead;
				byte[] data = new byte[1024];
				while ( ( nRead = input.read( data, 0, data.length ) ) != -1 ) {
					baos.write( data, 0, nRead );
				}
				data = baos.toByteArray();


				return defineClass( name, data, 0, data.length );
			}
			catch (IOException e) {
				throw new ClassNotFoundException( name, e );
			}
		}
	}
}
