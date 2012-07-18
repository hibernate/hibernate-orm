//$Id$
/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.jpa.test.connection;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.hibernate.cfg.Environment;
import org.hibernate.ejb.HibernatePersistence;
import org.hibernate.jpa.test.Distributor;
import org.hibernate.jpa.test.Item;
import org.hibernate.jpa.test.xml.Light;
import org.hibernate.jpa.test.xml.Lighter;

/**
 * @author Emmanuel Bernard
 */
public class PersistenceUnitInfoImpl implements PersistenceUnitInfo {
	private Properties properties = new Properties();
	private List<String> mappingFiles;
	private URL puRoot;

	public PersistenceUnitInfoImpl(URL puRoot, String[] mappingFiles) {
		this.mappingFiles = new ArrayList<String>( mappingFiles.length );
		this.mappingFiles.addAll( Arrays.asList( mappingFiles ) );
		this.puRoot = puRoot;
	}

	public String getPersistenceUnitName() {
		return "persistenceinfo";
	}

	public String getPersistenceProviderClassName() {
		return HibernatePersistence.class.getName();
	}

	public DataSource getJtaDataSource() {
		return new FakeDataSource();
	}

	public DataSource getNonJtaDataSource() {
		return null;
	}

	public List<String> getMappingFileNames() {
		return mappingFiles;
	}

	public List<URL> getJarFileUrls() {
		return new ArrayList<URL>();
	}

	public List<String> getManagedClassNames() {
		List<String> classes = new ArrayList<String>();
		classes.add( Item.class.getName() );
		classes.add( Distributor.class.getName() );
		classes.add( Light.class.getName() );
		classes.add( Lighter.class.getName() );
		return classes;
	}

	public Properties getProperties() {
		properties.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		return properties;
	}

	public String getPersistenceXMLSchemaVersion() {
		return null;
	}

	public ClassLoader getClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	public PersistenceUnitTransactionType getTransactionType() {
		return null;
	}

	public URL getPersistenceUnitRootUrl() {
		return puRoot;
	}

	public boolean excludeUnlistedClasses() {
		return true;
	}

	public SharedCacheMode getSharedCacheMode() {
		return null;
	}

	public ValidationMode getValidationMode() {
		return null;
	}

	public void addTransformer(ClassTransformer transformer) {
	}

	public ClassLoader getNewTempClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}
}
