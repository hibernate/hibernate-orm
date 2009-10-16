//$Id$
package org.hibernate.ejb.test.connection;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.sql.DataSource;

import org.hibernate.cfg.Environment;
import org.hibernate.ejb.HibernatePersistence;
import org.hibernate.ejb.test.Distributor;
import org.hibernate.ejb.test.Item;
import org.hibernate.ejb.test.xml.Light;
import org.hibernate.ejb.test.xml.Lighter;

/**
 * @author Emmanuel Bernard
 */
public class PersistenceUnitInfoImpl implements PersistenceUnitInfo {
	private Properties properties = new Properties();
	private List<String> mappingFiles;
	private URL puRoot;

	public PersistenceUnitInfoImpl(URL puRoot, String[] mappingFiles) {
		this.mappingFiles = new ArrayList<String>( mappingFiles.length );
		for ( String mappingFile : mappingFiles ) {
			this.mappingFiles.add( mappingFile );
		}
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
		return null;  //To change body of implemented methods use File | Settings | File Templates.
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
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public ValidationMode getValidationMode() {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public void addTransformer(ClassTransformer transformer) {
	}

	public ClassLoader getNewTempClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}
}
