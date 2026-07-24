/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.spi;

import org.hibernate.action.queue.spi.PlanningOptions;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.collection.spi.CollectionSemanticsResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.mapping.GeneratorSettings;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.internal.EmbeddableHandoffResolver;
import org.hibernate.metamodel.internal.IdentifierHandoffResolver;
import org.hibernate.metamodel.internal.MappedSuperclassHandoffResolver;
import org.hibernate.metamodel.internal.RuntimeModelHandoffResolvers;
import org.hibernate.metamodel.internal.RuntimeMappingHandoff;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.temporal.spi.ChangesetCoordinator;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.Map;

/**
 * @author Steve Ebersole
 */
public interface RuntimeModelCreationContext {
	SessionFactoryImplementor getSessionFactory();

	/**
	 * Access to the SessionFactory for runtime model objects that need the
	 * factory for later runtime behavior.
	 */
	default SessionFactoryAccess getSessionFactoryAccess() {
		return this::getSessionFactory;
	}

	BootstrapContext getBootstrapContext();

	default ModelsContext getModelsContext() {
		return getBootstrapContext().getModelsContext();
	}

	default ClassLoaderService getClassLoaderService() {
		return getBootstrapContext().getClassLoaderService();
	}

	default ClassLoaderAccess getClassLoaderAccess() {
		return getBootstrapContext().getClassLoaderAccess();
	}

	default ManagedBeanRegistry getManagedBeanRegistry() {
		return getBootstrapContext().getManagedBeanRegistry();
	}

	default MappingResolutionOptions getMappingResolutionOptions() {
		return getBootModel().getMappingResolutionOptions();
	}

	default ManagedTypeRepresentationResolver getRepresentationStrategySelector() {
		return getBootstrapContext().getRepresentationStrategySelector();
	}

	default CollectionSemanticsResolver getPersistentCollectionRepresentationResolver() {
		return getMappingResolutionOptions().getPersistentCollectionRepresentationResolver();
	}

	default BytecodeProvider getBytecodeProvider() {
		return getServiceRegistry().requireService( BytecodeProvider.class );
	}

	default ConfigurationService getConfigurationService() {
		return getServiceRegistry().requireService( ConfigurationService.class );
	}

	MetadataImplementor getBootModel();

	MappingMetamodelImplementor getDomainModel();

	TypeConfiguration getTypeConfiguration();

	PlanningOptions getGraphPlanningOptions();

	default JavaTypeRegistry getJavaTypeRegistry() {
		return getTypeConfiguration().getJavaTypeRegistry();
	}

	default MetadataImplementor getMetadata() {
		return getBootModel();
	}

	RuntimeMappingHandoff getRuntimeMappingHandoff();

	default RuntimeModelHandoffResolvers getHandoffResolvers() {
		return RuntimeModelHandoffResolvers.create( getRuntimeMappingHandoff() );
	}

	default MappedSuperclassHandoffResolver getMappedSuperclassHandoffResolver() {
		return getHandoffResolvers().mappedSuperclassHandoffResolver();
	}

	default EmbeddableHandoffResolver getEmbeddableHandoffResolver() {
		return getHandoffResolvers().embeddableHandoffResolver();
	}

	default IdentifierHandoffResolver getIdentifierHandoffResolver() {
		return getHandoffResolvers().identifierHandoffResolver();
	}

	SqmFunctionRegistry getFunctionRegistry();

	Map<String, Object> getSettings();

	Dialect getDialect();

	CacheImplementor getCache();

	SessionFactoryOptions getSessionFactoryOptions();

	JdbcServices getJdbcServices();

	SqlStringGenerationContext getSqlStringGenerationContext();

	WrapperOptions getWrapperOptions();

	ChangesetCoordinator getChangesetCoordinator();

	ServiceRegistry getServiceRegistry();

	Map<String, Generator> getGenerators();

	GeneratorSettings getGeneratorSettings();

	// For Hibernate Reactive
	Generator getOrCreateIdGenerator(String rootName, PersistentClass persistentClass);
}
