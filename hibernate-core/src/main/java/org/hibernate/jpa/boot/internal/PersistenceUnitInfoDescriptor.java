/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.boot.internal;

import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.internal.enhance.EnhancingClassTransformerImpl;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

/**
 * @author Steve Ebersole
 */
public class PersistenceUnitInfoDescriptor implements PersistenceUnitDescriptor {

	private static final CoreMessageLogger LOGGER = CoreLogging.messageLogger( PersistenceUnitInfoDescriptor.class );

	private final PersistenceUnitInfo persistenceUnitInfo;
	private ClassTransformer classTransformer;

	public PersistenceUnitInfoDescriptor(PersistenceUnitInfo persistenceUnitInfo) {
		this.persistenceUnitInfo = persistenceUnitInfo;
	}

	@Override
	public URL getPersistenceUnitRootUrl() {
		return persistenceUnitInfo.getPersistenceUnitRootUrl();
	}

	@Override
	public String getName() {
		return persistenceUnitInfo.getPersistenceUnitName();
	}

	@Override
	public Object getNonJtaDataSource() {
		return persistenceUnitInfo.getNonJtaDataSource();
	}

	@Override
	public Object getJtaDataSource() {
		return persistenceUnitInfo.getJtaDataSource();
	}

	@Override
	public String getProviderClassName() {
		return persistenceUnitInfo.getPersistenceProviderClassName();
	}

	@Override
	public PersistenceUnitTransactionType getTransactionType() {
		return persistenceUnitInfo.getTransactionType();
	}

	@Override
	public boolean isUseQuotedIdentifiers() {
		return false;
	}

	@Override
	public Properties getProperties() {
		return persistenceUnitInfo.getProperties();
	}

	@Override
	public ClassLoader getClassLoader() {
		return persistenceUnitInfo.getClassLoader();
	}

	@Override
	public ClassLoader getTempClassLoader() {
		return persistenceUnitInfo.getNewTempClassLoader();
	}

	@Override
	public boolean isExcludeUnlistedClasses() {
		return persistenceUnitInfo.excludeUnlistedClasses();
	}

	@Override
	public ValidationMode getValidationMode() {
		return persistenceUnitInfo.getValidationMode();
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return persistenceUnitInfo.getSharedCacheMode();
	}

	@Override
	public List<String> getManagedClassNames() {
		return persistenceUnitInfo.getManagedClassNames();
	}

	@Override
	public List<String> getMappingFileNames() {
		return persistenceUnitInfo.getMappingFileNames();
	}

	@Override
	public List<URL> getJarFileUrls() {
		return persistenceUnitInfo.getJarFileUrls();
	}

	@Override
	public void pushClassTransformer(EnhancementContext enhancementContext) {
		if ( this.classTransformer != null ) {
			throw new PersistenceException( "Persistence unit [" + persistenceUnitInfo.getPersistenceUnitName() + "] can only have a single class transformer." );
		}
		// During testing, we will return a null temp class loader in cases where we don't care about enhancement
		if ( persistenceUnitInfo.getNewTempClassLoader() != null ) {
			if ( LOGGER.isDebugEnabled() ) {
				LOGGER.debug( "Pushing class transformers for PU named '" + getName() + "' on loading classloader " + enhancementContext.getLoadingClassLoader() );
			}
			final EnhancingClassTransformerImpl classTransformer = new EnhancingClassTransformerImpl( enhancementContext );
			this.classTransformer = classTransformer;
			persistenceUnitInfo.addTransformer( classTransformer );
		}
	}

	@Override
	public ClassTransformer getClassTransformer() {
		return classTransformer;
	}
}
