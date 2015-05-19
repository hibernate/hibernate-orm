/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.jee;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.hibernate.AnnotationException;
import org.hibernate.InvalidMappingException;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.test.pack.defaultpar.Lighter;
import org.hibernate.jpa.test.pack.defaultpar_1_0.Lighter1;

import org.junit.Assert;
import org.junit.Test;

/**
 * "smoke" tests for JEE bootstrapping of HEM via a {@link PersistenceUnitInfo}
 *
 * @author Steve Ebersole
 */
public class OrmVersionTest {
    @Test
	public void testOrm1() {
		PersistenceUnitInfoImpl pui = new PersistenceUnitInfoImpl( "orm1-test", "1.0" )
				.addMappingFileName( "org/hibernate/jpa/test/jee/valid-orm-1.xml" );
		HibernatePersistenceProvider hp = new HibernatePersistenceProvider();
		EntityManagerFactory emf = hp.createContainerEntityManagerFactory( pui, Collections.EMPTY_MAP );
		emf.getMetamodel().entity( Lighter1.class ); // exception if not entity
	}
    @Test
	public void testOrm2() {
		PersistenceUnitInfoImpl pui = new PersistenceUnitInfoImpl( "orm2-test", "2.0" )
				.addMappingFileName( "org/hibernate/jpa/test/jee/valid-orm-2.xml" );
		HibernatePersistenceProvider hp = new HibernatePersistenceProvider();
		EntityManagerFactory emf = hp.createContainerEntityManagerFactory( pui, Collections.EMPTY_MAP );
		emf.getMetamodel().entity( Lighter.class ); // exception if not entity
	}
    @Test
	public void testInvalidOrm1() {
		PersistenceUnitInfoImpl pui = new PersistenceUnitInfoImpl( "invalid-orm1-test", "1.0" )
				.addMappingFileName( "org/hibernate/jpa/test/jee/invalid-orm-1.xml" );
		HibernatePersistenceProvider hp = new HibernatePersistenceProvider();
		try {
			hp.createContainerEntityManagerFactory( pui, Collections.EMPTY_MAP );
            Assert.fail( "expecting 'invalid content' error" );
		}
		catch ( InvalidMappingException expected ) {
			// expected condition
		}
		catch ( PersistenceException expected ) {
			// expected condition
		}
		catch (AnnotationException expected) {
			// expected condition
		}
	}

	public static class PersistenceUnitInfoImpl implements PersistenceUnitInfo {
		private final String name;
		private final String persistenceSchemaVersion;

		public PersistenceUnitInfoImpl(String name) {
			this( name, "2.0" );
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

		private final List<String> mappingFileNames = new ArrayList<String>();

		public List<String> getMappingFileNames() {
			return mappingFileNames;
		}

		private final List<String> managedClassNames = new ArrayList<String>();

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

		private final List<URL> jarFileUrls = new ArrayList<URL>();

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
			return getClassLoader();
		}
	}
}
