/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
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
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryParameterBindingTypeResolver;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
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
		extends Mapping, SessionFactory, SqmCreationContext, SqlAstCreationContext, QueryParameterBindingTypeResolver {
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
	Session openTemporarySession() throws HibernateException;

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
	IdentifierGenerator getIdentifierGenerator(String rootEntityName);


	EntityNotFoundDelegate getEntityNotFoundDelegate();

	void addObserver(SessionFactoryObserver observer);

	//todo make a Service ?
	CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy();

	//todo make a Service ?
	CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver();

	/**
	 * @return the FastSessionServices instance associated with this SessionFactory
	 */
	FastSessionServices getFastSessionServices();

	WrapperOptions getWrapperOptions();

	/**
	 * Contract for resolving this SessionFactory on deserialization
	 */
	interface DeserializationResolver<T extends SessionFactoryImplementor> extends Serializable {
		T resolve();
	}

	DeserializationResolver getDeserializationResolver();



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations

	/**
	 * Get the JdbcServices.
	 *
	 * @return the JdbcServices
	 */
	JdbcServices getJdbcServices();

	SqlStringGenerationContext getSqlStringGenerationContext();



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// map these to Metamodel

	/**
	 * @deprecated no longer for internal use, use {@link #getMappingMetamodel()} or {@link #getJpaMetamodel()}
	 */
	@Override @Deprecated
	MetamodelImplementor getMetamodel();

	RootGraphImplementor<?> findEntityGraphByName(String name);

	/**
	 * The best guess entity name for an entity not in an association
	 */
	String bestGuessEntityName(Object object);

}
