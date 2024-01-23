/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.Collection;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.event.spi.EventEngine;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.FastSessionServices;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeMetamodelsImplementor;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.BindableType;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryParameterBindingTypeResolver;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Defines the internal contract between the {@link SessionFactory} and the internal
 * implementation of Hibernate.
 *
 * @see SessionFactory
 * @see org.hibernate.internal.SessionFactoryImpl
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface SessionFactoryImplementor
		extends Mapping, SessionFactory, SqmCreationContext, SqlAstCreationContext,
				QueryParameterBindingTypeResolver { //deprecated extension, use MappingMetamodel
	/**
	 * Get the UUID for this SessionFactory.
	 * <p>
	 * The value is generated as a {@link java.util.UUID}, but kept as a String.
	 *
	 * @return The UUID for this SessionFactory.
	 *
	 * @see org.hibernate.internal.SessionFactoryRegistry#getSessionFactory
	 */
	String getUuid();

	/**
	 * Access to the name (if one) assigned to the SessionFactory
	 *
	 * @return The name for the SessionFactory
	 */
	String getName();

	/**
	 * Overrides {@link SessionFactory#openSession()} to widen the return type:
	 * this is useful for internal code depending on {@link SessionFactoryImplementor}
	 * as it would otherwise need to frequently resort to casting to the internal contract.
	 * @return the opened Session.
	 */
	@Override
	SessionImplementor openSession();

	TypeConfiguration getTypeConfiguration();

	default SessionFactoryImplementor getSessionFactory() {
		return this;
	}

	@Override
	default MappingMetamodelImplementor getMappingMetamodel() {
		return getRuntimeMetamodels().getMappingMetamodel();
	}

	QueryEngine getQueryEngine();

	@Override
	HibernateCriteriaBuilder getCriteriaBuilder();

	@Override
	SessionBuilderImplementor withOptions();

	/**
	 * Get a non-transactional "current" session (used by hibernate-envers)
	 */
	SessionImplementor openTemporarySession() throws HibernateException;

	@Override
	CacheImplementor getCache();

	@Override
	StatisticsImplementor getStatistics();

	RuntimeMetamodelsImplementor getRuntimeMetamodels();

	/**
	 * Access to the ServiceRegistry for this SessionFactory.
	 *
	 * @return The factory's ServiceRegistry
	 */
	ServiceRegistryImplementor getServiceRegistry();

	/**
	 * Get the EventEngine associated with this SessionFactory
	 */
	EventEngine getEventEngine();

	/**
	 * Retrieve fetch profile by name.
	 *
	 * @param name The name of the profile to retrieve.
	 * @return The profile definition
	 */
	FetchProfile getFetchProfile(String name);

	/**
	 * Get the identifier generator for the hierarchy
	 */
	Generator getGenerator(String rootEntityName);

	EntityNotFoundDelegate getEntityNotFoundDelegate();

	void addObserver(SessionFactoryObserver observer);

	//todo make a Service ?
	CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy();

	//todo make a Service ?
	CurrentTenantIdentifierResolver<Object> getCurrentTenantIdentifierResolver();

	/**
	 * The java type to use for a tenant identifier.
	 *
	 * @since 6.4
	 */
	JavaType<Object> getTenantIdentifierJavaType();

	/**
	 * @return the FastSessionServices instance associated with this SessionFactory
	 */
	FastSessionServices getFastSessionServices();

	WrapperOptions getWrapperOptions();

	SessionFactoryOptions getSessionFactoryOptions();

	FilterDefinition getFilterDefinition(String filterName);

	Collection<FilterDefinition> getAutoEnabledFilters();




	/**
	 * Get the JdbcServices.
	 *
	 * @return the JdbcServices
	 */
	JdbcServices getJdbcServices();

	SqlStringGenerationContext getSqlStringGenerationContext();



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// map these to Metamodel

	RootGraphImplementor<?> findEntityGraphByName(String name);

	/**
	 * The best guess entity name for an entity not in an association
	 */
	String bestGuessEntityName(Object object);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations

	/**
	 * Get the identifier generator for the hierarchy
	 *
	 * @deprecated use {@link #getGenerator(String)}
	 */
	@Deprecated(since = "6.2")
	IdentifierGenerator getIdentifierGenerator(String rootEntityName);

	/**
	 * Contract for resolving this SessionFactory on deserialization
	 *
	 * @deprecated this is no longer used
	 */
	@Deprecated(since = "6.2", forRemoval = true)
	interface DeserializationResolver<T extends SessionFactoryImplementor> extends Serializable {
		T resolve();
	}

	/**
	 * @deprecated this is never called
	 */
	@Deprecated(since = "6.2", forRemoval = true)
	DeserializationResolver<?> getDeserializationResolver();

	/**
	 * @deprecated no longer for internal use, use {@link #getMappingMetamodel()} or {@link #getJpaMetamodel()}
	 */
	@Override @Deprecated
	MetamodelImplementor getMetamodel();

	/**
	 * @deprecated Use {@link #getMappingMetamodel()}.{@link MappingMetamodelImplementor#resolveParameterBindType(Object)}
	 */
	@Override @Deprecated(since = "6.2", forRemoval = true)
	<T> BindableType<? super T> resolveParameterBindType(T bindValue);

	/**
	 * @deprecated Use {@link #getMappingMetamodel()}.{@link MappingMetamodelImplementor#resolveParameterBindType(Class)}
	 */
	@Override @Deprecated(since = "6.2", forRemoval = true)
	<T> BindableType<T> resolveParameterBindType(Class<T> clazz);

}
