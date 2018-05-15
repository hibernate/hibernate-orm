/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityGraph;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.Metamodel;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cfg.Settings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.spi.NamedQueryRepository;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryParameterBindingTypeResolver;
import org.hibernate.query.spi.ResultSetMappingDescriptor;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Defines the internal contract between the <tt>SessionFactory</tt> and other parts of
 * Hibernate such as implementors of <tt>Type</tt>.
 *
 * @see org.hibernate.SessionFactory
 * @see org.hibernate.internal.SessionFactoryImpl
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface SessionFactoryImplementor
		extends SessionFactory, QueryParameterBindingTypeResolver {
	/**
	 * Get the UUID for this SessionFactory.  The value is generated as a {@link java.util.UUID}, but kept
	 * as a String.
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

	/**
	 * Access to the ServiceRegistry for this SessionFactory.
	 *
	 * @return The factory's ServiceRegistry
	 */
	ServiceRegistryImplementor getServiceRegistry();

	/**
	 * Get the factory scoped interceptor for this factory.
	 *
	 * @return The factory scope interceptor, or null if none.
	 *
	 * @deprecated (since 5.2) if access to the SessionFactory-scoped Interceptor is needed, use
	 * {@link SessionFactoryOptions#getInterceptor()} instead.  However, generally speaking this access
	 * is not needed.
	 */
	@Deprecated
	Interceptor getInterceptor();

	QueryEngine getQueryEngine();

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

	/**
	 * @deprecated use {@link QueryEngine#getSqmFunctionRegistry()} instead
	 */
	@Deprecated
	default SqmFunctionRegistry getSqmFunctionRegistry() {
		return getQueryEngine().getSqmFunctionRegistry();
	}


	void addObserver(SessionFactoryObserver observer);

	/**
	 * @todo make a Service ?
	 */
	CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy();

	/**
	 * @todo make a Service ?
	 */
	CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver();

	/**
	 * Contract for resolving this SessionFactory on deserialization
	 */
	@FunctionalInterface
	interface DeserializationResolver<T extends SessionFactoryImplementor> extends Serializable {
		T resolve();
	}

	DeserializationResolver getDeserializationResolver();

	@Override
	MetamodelImplementor getMetamodel();

	/**
	 * Find all  {@code EntityGraph}s associated with a given entity type.
	 *
	 * @param entityClass the entity type for which to find all {@code EntityGraph}s.
	 *
	 * @return A list of {@code EntityGraph} instances associated with the given entity type. The empty list is
	 * returned in case there are not entity graphs.
	 */
	@SuppressWarnings("unchecked")
	default <T> List<EntityGraph<? super T>> findEntityGraphsByType(Class<T> entityClass) {
		return (List) findEntityGraphsByJavaType( entityClass );
	}

	<T> List<RootGraphImplementor<? super T>> findEntityGraphsByJavaType(Class<T> entityClass);

	RootGraphImplementor<?> findEntityGraphByName(String name);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations

	/**
	 * Get the return types of a query
	 *
	 * @deprecated No replacement.
	 */
	@Deprecated
	default Type[] getReturnTypes(String queryString) {
		throw new UnsupportedOperationException( "Concept of query return org.hibernate.type.Types is no longer supported" );
	}

	/**
	 * Get the return aliases of a query
	 *
	 * @deprecated No replacement.
	 */
	@Deprecated
	default String[] getReturnAliases(String queryString) {
		throw new UnsupportedOperationException( "Access to of query return aliases via Sessionfactory is no longer supported" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NamedQueryRepository

	/**
	 * @deprecated (since 5.2) Use {@link NamedQueryRepository#getResultSetMappingDescriptor} instead.
	 */
	@Deprecated
	default ResultSetMappingDescriptor getResultSetMapping(String name) {
		return getQueryEngine().getNamedQueryRepository().getResultSetMappingDescriptor( name );
	}

	/**
	 * Get the JdbcServices.
	 *
	 * @return the JdbcServices
	 */
	JdbcServices getJdbcServices();

	/**
	 * Get the SQL dialect.
	 * <p/>
	 * Shorthand for {@code getJdbcServices().getDialect()}
	 *
	 * @return The dialect
	 *
	 * @deprecated (since 5.2) instead, use this factory's {{@link #getServiceRegistry()}} ->
	 * {@link JdbcServices#getDialect()}
	 */
	@Deprecated
	default Dialect getDialect() {
		if ( getServiceRegistry() == null ) {
			throw new IllegalStateException( "Cannot determine dialect because serviceRegistry is null." );
		}

		return getServiceRegistry().getService( JdbcServices.class ).getDialect();
	}

	/**
	 * Retrieves the SQLExceptionConverter in effect for this SessionFactory.
	 *
	 * @return The SQLExceptionConverter for this SessionFactory.
	 *
	 * @deprecated since 5.0; use {@link JdbcServices#getSqlExceptionHelper()} ->
	 * {@link SqlExceptionHelper#getSqlExceptionConverter()} instead as obtained from {@link #getServiceRegistry()}
	 */
	@Deprecated
	default SQLExceptionConverter getSQLExceptionConverter() {
		return getServiceRegistry().getService( JdbcServices.class ).getSqlExceptionHelper().getSqlExceptionConverter();
	}

	/**
	 * Retrieves the SqlExceptionHelper in effect for this SessionFactory.
	 *
	 * @return The SqlExceptionHelper for this SessionFactory.
	 *
	 * @deprecated since 5.0; use {@link JdbcServices#getSqlExceptionHelper()} instead as
	 * obtained from {@link #getServiceRegistry()}
	 */
	@Deprecated
	default SqlExceptionHelper getSQLExceptionHelper() {
		return getServiceRegistry().getService( JdbcServices.class ).getSqlExceptionHelper();
	}

	/**
	 * @deprecated since 5.0; use {@link #getSessionFactoryOptions()} instead
	 */
	@Deprecated
	@SuppressWarnings("deprecation")
	Settings getSettings();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations (5.2)
	//		todo : remove these deprecations

	/**
	 * @deprecated (since 5.2) Use
	 * {@link #getMetamodel()}} -> {@link Metamodel#findEntityDescriptor(String)}
	 * instead
	 */
	@Deprecated
	default EntityTypeDescriptor getEntityPersister(String entityName) throws MappingException {
		return getMetamodel().getEntityDescriptor( entityName );
	}

	/**
	 * @deprecated (since 5.2) Use
	 * {@link #getMetamodel()}} -> {@link Metamodel#findCollectionDescriptor(String)} instead
	 */
	@Deprecated
	default PersistentCollectionDescriptor getCollectionPersister(String role) throws MappingException {
		final PersistentCollectionDescriptor<?,?,?> collectionDescriptor = getMetamodel().findCollectionDescriptor( role );
		if ( collectionDescriptor == null ) {
			throw new MappingException( "Could not locate CollectionPersister for given role [" + role + "]" );
		}
		return collectionDescriptor;
	}

	/**
	 * @deprecated (since 5.2) Use
	 * {@link #getMetamodel()}} -> {@link MetamodelImplementor#findCollectionRolesByEntityParticipant}
	 * instead
	 */
	@Deprecated
	default Set<String> getCollectionRolesByEntityParticipant(String entityName) {
		return getMetamodel().findCollectionRolesByEntityParticipant( getMetamodel().findEntityDescriptor( entityName ) );
	}

	/**
	 * @deprecated (since 5.2) Use
	 * {@link #getMetamodel()}} -> {@link Metamodel#getEntityDescriptor(Class)}
	 * instead
	 */
	@Deprecated
	default EntityTypeDescriptor locateEntityPersister(Class byClass) {
		return getMetamodel().getEntityDescriptor( byClass );
	}

	/**
	 * @deprecated (since 5.2) Use
	 * {@link #getMetamodel()}} -> {@link Metamodel#findEntityDescriptor(String)}
	 * instead
	 */
	@Deprecated
	default EntityTypeDescriptor locateEntityPersister(String byName) {
		return getMetamodel().findEntityDescriptor( byName );
	}
}
