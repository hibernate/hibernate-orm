/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.boot;

import java.util.Collection;
import java.util.Map;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.AttributeConverterInfo;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.jpa.spi.MutableJpaCompliance;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.jandex.IndexView;

/**
 * @author Andrea Boriero
 */
public class BootstrapContextImpl implements BootstrapContext {
	public static final BootstrapContextImpl INSTANCE = new BootstrapContextImpl();

	private BootstrapContext delegate;

	private BootstrapContextImpl() {
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build();
		MetadataBuildingOptions buildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );

		delegate = new org.hibernate.boot.internal.BootstrapContextImpl( serviceRegistry, buildingOptions );
	}

	@Override
	public StandardServiceRegistry getServiceRegistry() {
		return delegate.getServiceRegistry();
	}

	@Override
	public MutableJpaCompliance getJpaCompliance() {
		return delegate.getJpaCompliance();
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return delegate.getTypeConfiguration();
	}

	@Override
	public MetadataBuildingOptions getMetadataBuildingOptions() {
		return delegate.getMetadataBuildingOptions();
	}

	@Override
	public boolean isJpaBootstrap() {
		return delegate.isJpaBootstrap();
	}

	@Override
	public void markAsJpaBootstrap() {
		delegate.markAsJpaBootstrap();
	}

	@Override
	public ClassLoader getJpaTempClassLoader() {
		return delegate.getJpaTempClassLoader();
	}

	@Override
	public ClassLoaderAccess getClassLoaderAccess() {
		return delegate.getClassLoaderAccess();
	}

	@Override
	public ClassmateContext getClassmateContext() {
		return delegate.getClassmateContext();
	}

	@Override
	public ArchiveDescriptorFactory getArchiveDescriptorFactory() {
		return delegate.getArchiveDescriptorFactory();
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
	public ReflectionManager getReflectionManager() {
		return delegate.getReflectionManager();
	}

	@Override
	public IndexView getJandexView() {
		return delegate.getJandexView();
	}

	@Override
	public Map<String, SQLFunction> getSqlFunctions() {
		return delegate.getSqlFunctions();
	}

	@Override
	public Collection<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjectList() {
		return delegate.getAuxiliaryDatabaseObjectList();
	}

	@Override
	public Collection<AttributeConverterInfo> getAttributeConverters() {
		return delegate.getAttributeConverters();
	}

	@Override
	public Collection<CacheRegionDefinition> getCacheRegionDefinitions() {
		return delegate.getCacheRegionDefinitions();
	}

	@Override
	public void release() {
		delegate.release();
	}
}
