/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.xml.versions;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.hibernate.AnnotationException;
import org.hibernate.InvalidMappingException;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.orm.test.jpa.pack.defaultpar.Lighter;
import org.hibernate.orm.test.jpa.pack.defaultpar_1_0.Lighter1;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * "smoke" tests for JEE bootstrapping of HEM via a {@link PersistenceUnitInfo}
 *
 * @author Steve Ebersole
 */
public class JpaXsdVersionsTest {
    @Test
	public void testOrm1() {
		PersistenceUnitInfoImpl pui = new PersistenceUnitInfoImpl( "orm1-test", "1.0" )
				.addMappingFileName( "org/hibernate/orm/test/jpa/xml/versions/valid-orm-1_0.xml" );
		HibernatePersistenceProvider hp = new HibernatePersistenceProvider();
		try ( EntityManagerFactory emf = hp.createContainerEntityManagerFactory( pui, getDefaultPuConfig() ) ) {
			emf.getMetamodel().entity(Lighter1.class); // exception if not entity
		}
	}

	private static Map<Object, Object> getDefaultPuConfig() {
		final Map<Object, Object> settings = new HashMap<>();
		ServiceRegistryUtil.applySettings( settings );
		return settings;
	}

	@Test
	public void testOrm20() {
		PersistenceUnitInfoImpl pui = new PersistenceUnitInfoImpl( "orm2-test", "2.0" )
				.addMappingFileName( "org/hibernate/orm/test/jpa/xml/versions/valid-orm-2_0.xml" );
		HibernatePersistenceProvider hp = new HibernatePersistenceProvider();
		try ( EntityManagerFactory emf = hp.createContainerEntityManagerFactory( pui, getDefaultPuConfig() ) ) {
			emf.getMetamodel().entity(Lighter.class); // exception if not entity
		}
	}

    @Test
	public void testOrm21() {
		PersistenceUnitInfoImpl pui = new PersistenceUnitInfoImpl( "orm2-test", "2.1" )
				.addMappingFileName( "org/hibernate/orm/test/jpa/xml/versions/valid-orm-2_1.xml" );
		HibernatePersistenceProvider hp = new HibernatePersistenceProvider();
		try ( EntityManagerFactory emf = hp.createContainerEntityManagerFactory( pui, getDefaultPuConfig() ) ) {
			emf.getMetamodel().entity(Lighter.class); // exception if not entity
		}
	}

    @Test
	public void testOrm22() {
		PersistenceUnitInfoImpl pui = new PersistenceUnitInfoImpl( "orm2-test", "2.2")
				.addMappingFileName( "org/hibernate/orm/test/jpa/xml/versions/valid-orm-2_2.xml" );
		HibernatePersistenceProvider hp = new HibernatePersistenceProvider();
		try ( EntityManagerFactory emf = hp.createContainerEntityManagerFactory( pui, getDefaultPuConfig() ) ) {
			emf.getMetamodel().entity(Lighter.class); // exception if not entity
		}
	}

    @Test
	public void testInvalidOrm1() {
		PersistenceUnitInfoImpl pui = new PersistenceUnitInfoImpl( "invalid-orm1-test", "1.0" )
				.addMappingFileName( "org/hibernate/orm/test/jpa/xml/versions/invalid-orm-1_0.xml" );
		HibernatePersistenceProvider hp = new HibernatePersistenceProvider();
		try ( EntityManagerFactory emf = hp.createContainerEntityManagerFactory( pui, getDefaultPuConfig() ) ) {
			Assert.fail("expecting 'invalid content' error");
		}
		catch (InvalidMappingException | AnnotationException expected) {
			// expected condition
		}
		catch (PersistenceException expected) {
			// expected condition
		}
	}

	public static class PersistenceUnitInfoImpl implements PersistenceUnitInfo {
		private final String name;
		private final String persistenceSchemaVersion;

		public PersistenceUnitInfoImpl(String name) {
			this( name, "2.2" );
		}

		public PersistenceUnitInfoImpl(String name, String persistenceSchemaVersion) {
			this.name = name;
			this.persistenceSchemaVersion = persistenceSchemaVersion;
		}

		public String getPersistenceUnitName() {
			return name;
		}

		public String getPersistenceXMLSchemaVersion() {
			return persistenceSchemaVersion;
		}

		@Override
		public String getScopeAnnotationName() {
			return null;
		}

		@Override
		public List<String> getQualifierAnnotationNames() {
			return List.of();
		}

		private final List<String> mappingFileNames = new ArrayList<>();

		public List<String> getMappingFileNames() {
			return mappingFileNames;
		}

		private final List<String> managedClassNames = new ArrayList<>();

		private PersistenceUnitInfoImpl addMappingFileName(String mappingFileName) {
			mappingFileNames.add( mappingFileName );
			return this;
		}

		public List<String> getManagedClassNames() {
			return managedClassNames;
		}

		public String getPersistenceProviderClassName() {
			return null;
		}

		public PersistenceUnitTransactionType getTransactionType() {
			return PersistenceUnitTransactionType.RESOURCE_LOCAL;
		}

		public DataSource getJtaDataSource() {
			return null;
		}

		public DataSource getNonJtaDataSource() {
			return null;
		}

		private final List<URL> jarFileUrls = new ArrayList<>();

		public List<URL> getJarFileUrls() {
			return jarFileUrls;
		}

		public URL getPersistenceUnitRootUrl() {
			return null;
		}

		public boolean excludeUnlistedClasses() {
			return false;
		}

		public SharedCacheMode getSharedCacheMode() {
			return null;
		}

		public ValidationMode getValidationMode() {
			return null;
		}

		private final Properties properties = new Properties();

		public Properties getProperties() {
			return properties;
		}

		public ClassLoader getClassLoader() {
			return Thread.currentThread().getContextClassLoader();
		}

		public void addTransformer(ClassTransformer transformer) {
		}

		public ClassLoader getNewTempClassLoader() {
			return null;
		}
	}
}
