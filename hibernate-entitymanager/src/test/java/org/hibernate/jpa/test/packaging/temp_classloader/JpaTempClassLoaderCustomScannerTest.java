package org.hibernate.jpa.test.packaging.temp_classloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
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

}
