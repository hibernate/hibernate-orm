/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.InvalidMappingException;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.test.pack.defaultpar.Lighter;
import org.hibernate.jpa.test.pack.defaultpar_1_0.Lighter1;

import org.hibernate.testing.FailureExpectedWithNewUnifiedXsd;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * "smoke" tests for JEE bootstrapping of HEM via a {@link PersistenceUnitInfo}
 *
 * @author Steve Ebersole
 */
public class OrmVersionTest extends BaseUnitTestCase {
    @Test
	@FailureExpectedWithNewUnifiedXsd
    public void testOrm1() {
		PersistenceUnitInfoImpl pui = new PersistenceUnitInfoImpl( "orm1-test", "1.0" )
				.addMappingFileName( "org/hibernate/jpa/test/jee/valid-orm-1.xml" );
		HibernatePersistenceProvider hp = new HibernatePersistenceProvider();
		EntityManagerFactory emf = hp.createContainerEntityManagerFactory( pui, Collections.EMPTY_MAP );
		emf.getMetamodel().entity( Lighter1.class ); // exception if not entity
		emf.close();
	}

    @Test
    @FailureExpectedWithNewUnifiedXsd
    public void testOrm2() {
		PersistenceUnitInfoImpl pui = new PersistenceUnitInfoImpl( "orm2-test", "2.0" )
				.addMappingFileName( "org/hibernate/jpa/test/jee/valid-orm-2.xml" );
		HibernatePersistenceProvider hp = new HibernatePersistenceProvider();
		EntityManagerFactory emf = hp.createContainerEntityManagerFactory( pui, Collections.EMPTY_MAP );
		emf.getMetamodel().entity( Lighter.class ); // exception if not entity
		emf.close();
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

		private PersistenceUnitInfoImpl addMappingFileName(String mappingFileName) {
			mappingFileNames.add( mappingFileName );
			return this;
		}

		private final List<String> managedClassNames = new ArrayList<String>();

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
