/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.spi;

import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.mapping.GeneratorSettings;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.Map;

/**
 * @author Steve Ebersole
 */
public interface RuntimeModelCreationContext {
	SessionFactoryImplementor getSessionFactory();

	BootstrapContext getBootstrapContext();

	MetadataImplementor getBootModel();

	MappingMetamodelImplementor getDomainModel();

	TypeConfiguration getTypeConfiguration();

	default JavaTypeRegistry getJavaTypeRegistry() {
		return getTypeConfiguration().getJavaTypeRegistry();
	}

	default MetadataImplementor getMetadata() {
		return getBootModel();
	}

	SqmFunctionRegistry getFunctionRegistry();

	Map<String, Object> getSettings();

	Dialect getDialect();

	CacheImplementor getCache();

	SessionFactoryOptions getSessionFactoryOptions();

	JdbcServices getJdbcServices();

	SqlStringGenerationContext getSqlStringGenerationContext();

	ServiceRegistry getServiceRegistry();

	Map<String, Generator> getGenerators();

	GeneratorSettings getGeneratorSettings();

	// For Hibernate Reactive
	Generator getOrCreateIdGenerator(String rootName, PersistentClass persistentClass);
}
