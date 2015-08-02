/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import java.util.List;
import java.util.Map;
import javax.persistence.SharedCacheMode;

import org.hibernate.HibernateException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.model.IdGeneratorStrategyInterpreter;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.cfg.MetadataSourceType;
import org.hibernate.dialect.function.SQLFunction;

import org.jboss.jandex.IndexView;

/**
 * Convenience base class for custom implementors of {@link MetadataBuildingOptions} using delegation.
 *
 * @author Gunnar Morling
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public abstract class AbstractDelegatingMetadataBuildingOptions implements MetadataBuildingOptions, JpaOrmXmlPersistenceUnitDefaultAware {

	private final MetadataBuildingOptions delegate;

	public AbstractDelegatingMetadataBuildingOptions(MetadataBuildingOptions delegate) {
		this.delegate = delegate;
	}

	@Override
	public StandardServiceRegistry getServiceRegistry() {
		return delegate.getServiceRegistry();
	}

	@Override
	public MappingDefaults getMappingDefaults() {
		return delegate.getMappingDefaults();
	}

	@Override
	public List<BasicTypeRegistration> getBasicTypeRegistrations() {
		return delegate.getBasicTypeRegistrations();
	}

	@Override
	public IndexView getJandexView() {
		return delegate.getJandexView();
	}

	@Override
	public ScanOptions getScanOptions() {
		return delegate.getScanOptions();
	}

	@Override
	public ScanEnvironment getScanEnvironment() {
		return delegate.getScanEnvironment();
	}

	@Override
	public Object getScanner() {
		return delegate.getScanner();
	}

	@Override
	public ArchiveDescriptorFactory getArchiveDescriptorFactory() {
		return delegate.getArchiveDescriptorFactory();
	}

	@Override
	public ClassLoader getTempClassLoader() {
		return delegate.getTempClassLoader();
	}

	@Override
	public ImplicitNamingStrategy getImplicitNamingStrategy() {
		return delegate.getImplicitNamingStrategy();
	}

	@Override
	public PhysicalNamingStrategy getPhysicalNamingStrategy() {
		return delegate.getPhysicalNamingStrategy();
	}

	@Override
	public ReflectionManager getReflectionManager() {
		return delegate.getReflectionManager();
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return delegate.getSharedCacheMode();
	}

	@Override
	public AccessType getImplicitCacheAccessType() {
		return delegate.getImplicitCacheAccessType();
	}

	@Override
	public MultiTenancyStrategy getMultiTenancyStrategy() {
		return delegate.getMultiTenancyStrategy();
	}

	@Override
	public IdGeneratorStrategyInterpreter getIdGenerationTypeInterpreter() {
		return delegate.getIdGenerationTypeInterpreter();
	}

	@Override
	public List<CacheRegionDefinition> getCacheRegionDefinitions() {
		return delegate.getCacheRegionDefinitions();
	}

	@Override
	public boolean ignoreExplicitDiscriminatorsForJoinedInheritance() {
		return delegate.ignoreExplicitDiscriminatorsForJoinedInheritance();
	}

	@Override
	public boolean createImplicitDiscriminatorsForJoinedInheritance() {
		return delegate.createImplicitDiscriminatorsForJoinedInheritance();
	}

	@Override
	public boolean shouldImplicitlyForceDiscriminatorInSelect() {
		return delegate.shouldImplicitlyForceDiscriminatorInSelect();
	}

	@Override
	public boolean useNationalizedCharacterData() {
		return delegate.useNationalizedCharacterData();
	}

	@Override
	public boolean isSpecjProprietarySyntaxEnabled() {
		return delegate.isSpecjProprietarySyntaxEnabled();
	}

	@Override
	public List<MetadataSourceType> getSourceProcessOrdering() {
		return delegate.getSourceProcessOrdering();
	}

	@Override
	public Map<String, SQLFunction> getSqlFunctions() {
		return delegate.getSqlFunctions();
	}

	@Override
	public List<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjectList() {
		return delegate.getAuxiliaryDatabaseObjectList();
	}

	@Override
	public List<AttributeConverterDefinition> getAttributeConverters() {
		return delegate.getAttributeConverters();
	}

	@Override
	public void apply(JpaOrmXmlPersistenceUnitDefaults jpaOrmXmlPersistenceUnitDefaults) {
		if ( delegate instanceof JpaOrmXmlPersistenceUnitDefaultAware ) {
			( (JpaOrmXmlPersistenceUnitDefaultAware) delegate ).apply( jpaOrmXmlPersistenceUnitDefaults );
		}
		else {
			throw new HibernateException(
					"AbstractDelegatingMetadataBuildingOptions delegate did not " +
							"implement JpaOrmXmlPersistenceUnitDefaultAware; " +
							"cannot delegate JpaOrmXmlPersistenceUnitDefaultAware#apply"
			);
		}
	}
}
