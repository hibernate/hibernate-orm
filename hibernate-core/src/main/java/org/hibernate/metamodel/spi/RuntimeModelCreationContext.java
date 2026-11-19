package org.hibernate.metamodel.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.action.queue.spi.PlanningOptions;
import org.hibernate.Remove;
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
	@Remove
	@Nonnull
	SessionFactoryImplementor getSessionFactory();

	@Remove
	@Nonnull
	BootstrapContext getBootstrapContext();

	@Nonnull
	MetadataImplementor getBootModel();

	@Nonnull
	MappingMetamodelImplementor getDomainModel();

	@Nonnull
	TypeConfiguration getTypeConfiguration();

	@Nonnull
	PlanningOptions getGraphPlanningOptions();

	@Nonnull
	default JavaTypeRegistry getJavaTypeRegistry() {
		return getTypeConfiguration().getJavaTypeRegistry();
	}

	@Nonnull
	default MetadataImplementor getMetadata() {
		return getBootModel();
	}

	@Nonnull
	SqmFunctionRegistry getFunctionRegistry();

	@Nonnull
	Map<String, Object> getSettings();

	@Nonnull
	Dialect getDialect();

	@Nonnull
	CacheImplementor getCache();

	@Nonnull
	SessionFactoryOptions getSessionFactoryOptions();

	@Nonnull
	JdbcServices getJdbcServices();

	@Nonnull
	SqlStringGenerationContext getSqlStringGenerationContext();

	@Nonnull
	ServiceRegistry getServiceRegistry();

	@Nonnull
	Map<String, Generator> getGenerators();

	@Nonnull
	GeneratorSettings getGeneratorSettings();

	// For Hibernate Reactive
	@Nullable
	Generator getOrCreateIdGenerator(@Nonnull String rootName, @Nonnull PersistentClass persistentClass);
}
