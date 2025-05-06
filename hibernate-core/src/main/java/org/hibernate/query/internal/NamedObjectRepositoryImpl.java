/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.QueryException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.NamedProcedureCallDefinition;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.query.hql.spi.SqmQueryImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.sql.spi.NativeQueryImplementor;
import org.hibernate.query.sqm.EntityTypeException;
import org.hibernate.query.NamedQueryValidationException;
import org.hibernate.query.sqm.PathElementException;
import org.hibernate.query.sqm.TerminalPathException;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.sql.spi.NamedNativeQueryMemento;
import org.hibernate.query.sqm.UnknownEntityException;
import org.hibernate.query.sqm.UnknownPathException;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;

import org.jboss.logging.Logger;

import jakarta.persistence.TypedQueryReference;

import static org.hibernate.query.QueryLogging.QUERY_MESSAGE_LOGGER;

/**
 * @author Steve Ebersole
 */
public class NamedObjectRepositoryImpl implements NamedObjectRepository {
	private static final Logger log = Logger.getLogger( NamedObjectRepository.class );

	private final Map<String, NamedSqmQueryMemento<?>> sqmMementoMap;
	private final Map<String, NamedNativeQueryMemento<?>> sqlMementoMap;
	private final Map<String, NamedCallableQueryMemento> callableMementoMap;
	private final Map<String, NamedResultSetMappingMemento> resultSetMappingMementoMap;

	public NamedObjectRepositoryImpl(
			Map<String,NamedSqmQueryMemento<?>> sqmMementoMap,
			Map<String,NamedNativeQueryMemento<?>> sqlMementoMap,
			Map<String,NamedCallableQueryMemento> callableMementoMap,
			Map<String,NamedResultSetMappingMemento> resultSetMappingMementoMap) {
		this.sqmMementoMap = sqmMementoMap;
		this.sqlMementoMap = sqlMementoMap;
		this.callableMementoMap = callableMementoMap;
		this.resultSetMappingMementoMap = resultSetMappingMementoMap;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> Map<String, TypedQueryReference<R>> getNamedQueries(Class<R> resultType) {
		final Map<String, TypedQueryReference<R>> namedQueries = new HashMap<>( sqmMementoMap.size() + sqlMementoMap.size() );
		for ( Map.Entry<String, NamedSqmQueryMemento<?>> entry : sqmMementoMap.entrySet() ) {
			if ( resultType == entry.getValue().getResultType() ) {
				namedQueries.put( entry.getKey(), (TypedQueryReference<R>) entry.getValue() );
			}
		}
		for ( Map.Entry<String, NamedNativeQueryMemento<?>> entry : sqlMementoMap.entrySet() ) {
			if ( resultType == entry.getValue().getResultType() ) {
				namedQueries.put( entry.getKey(), (TypedQueryReference<R>) entry.getValue() );
			}
		}
		return namedQueries;
	}

	@Override
	public void registerNamedQuery(String name, Query query) {
		// use unwrap() here instead of 'instanceof' because the Query might be wrapped

		// first, handle stored procedures
		try {
			final var unwrapped = query.unwrap( ProcedureCallImplementor.class );
			if ( unwrapped != null ) {
				registerCallableQueryMemento( name, unwrapped.toMemento( name ) );
				return;
			}
		}
		catch ( PersistenceException ignore ) {
			// this means 'query' is not a ProcedureCallImplementor
		}

		// then try as a native SQL or JPQL query
		try {
			final var queryImplementor = query.unwrap( QueryImplementor.class );
			if ( queryImplementor != null ) {
				if ( queryImplementor instanceof NativeQueryImplementor<?> nativeQueryImplementor ) {
					registerNativeQueryMemento( name, nativeQueryImplementor.toMemento( name ) );

				}
				else if ( queryImplementor instanceof SqmQueryImplementor<?> sqmQueryImplementor ) {
					registerSqmQueryMemento( name, sqmQueryImplementor.toMemento( name ) );
				}
				else {
					throw new AssertionFailure( "unknown QueryImplementor" );
				}
				return;
			}
		}
		catch ( PersistenceException ignore ) {
			// this means 'query' is not a native SQL or JPQL query
		}

		throw new PersistenceException( "Could not register named query: " + name );
	}

	@Override
	public <R> TypedQueryReference<R> registerNamedQuery(String name, TypedQuery<R> query) {
		if ( query instanceof NativeQueryImplementor<R> nativeQueryImplementor ) {
			final NamedNativeQueryMemento<R> memento = nativeQueryImplementor.toMemento( name );
			registerNativeQueryMemento( name, memento );
			return memento;
		}
		else if ( query instanceof SqmQueryImplementor<R> sqmQueryImplementor ) {
			final NamedSqmQueryMemento<R> memento = sqmQueryImplementor.toMemento( name );
			registerSqmQueryMemento( name, memento );
			return memento;
		}
		else {
			throw new IllegalArgumentException( "unknown implementation of TypedQuery" );
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named SQM Memento

	@Override
	public NamedSqmQueryMemento<?> getSqmQueryMemento(String queryName) {
		return sqmMementoMap.get( queryName );
	}

	@Override
	public void visitSqmQueryMementos(Consumer<NamedSqmQueryMemento<?>> action) {
		sqmMementoMap.values().forEach( action );
	}

	@Override
	public void registerSqmQueryMemento(String name, NamedSqmQueryMemento<?> descriptor) {
		sqmMementoMap.put( name, descriptor );
		sqlMementoMap.remove( name );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SQL mementos

	@Override
	public NamedNativeQueryMemento<?> getNativeQueryMemento(String queryName) {
		return sqlMementoMap.get( queryName );
	}

	@Override
	public void visitNativeQueryMementos(Consumer<NamedNativeQueryMemento<?>> action) {
		sqlMementoMap.values().forEach( action );
	}

	@Override
	public synchronized void registerNativeQueryMemento(String name, NamedNativeQueryMemento<?> descriptor) {
		sqlMementoMap.put( name, descriptor );
		sqmMementoMap.remove( name );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// callable mementos

	@Override
	public NamedCallableQueryMemento getCallableQueryMemento(String name) {
		return callableMementoMap.get( name );
	}

	@Override
	public void visitCallableQueryMementos(Consumer<NamedCallableQueryMemento> action) {
		callableMementoMap.values().forEach( action );
	}

	@Override
	public synchronized void registerCallableQueryMemento(String name, NamedCallableQueryMemento memento) {
		callableMementoMap.put( name, memento );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Result-set mapping mementos

	@Override
	public NamedResultSetMappingMemento getResultSetMappingMemento(String mappingName) {
		return resultSetMappingMementoMap.get( mappingName );
	}

	@Override
	public void visitResultSetMappingMementos(Consumer<NamedResultSetMappingMemento> action) {
		resultSetMappingMementoMap.values().forEach( action );
	}

	@Override
	public void registerResultSetMappingMemento(String name, NamedResultSetMappingMemento memento) {
		resultSetMappingMementoMap.put( name, memento );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Prepare repository for use

	@Override
	public NamedQueryMemento<?> resolve(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor bootMetamodel,
			String registrationName) {
		NamedQueryMemento<?> namedQuery = sqlMementoMap.get( registrationName );
		if ( namedQuery != null ) {
			return namedQuery;
		}
		namedQuery = sqmMementoMap.get( registrationName );
		if ( namedQuery != null ) {
			return namedQuery;
		}
		namedQuery = callableMementoMap.get( registrationName );
		if ( namedQuery != null ) {
			return namedQuery;
		}
		final NamedHqlQueryDefinition<?> namedHqlQueryDefinition = bootMetamodel.getNamedHqlQueryMapping( registrationName );
		if ( namedHqlQueryDefinition != null ) {
			final NamedSqmQueryMemento<?> resolved = namedHqlQueryDefinition.resolve( sessionFactory );
			sqmMementoMap.put( namedHqlQueryDefinition.getRegistrationName(), resolved );
			return resolved;
		}
		final NamedNativeQueryDefinition<?> namedNativeQueryDefinition = bootMetamodel.getNamedNativeQueryMapping( registrationName );
		if ( namedNativeQueryDefinition != null ) {
			final NamedNativeQueryMemento<?> resolved = namedNativeQueryDefinition.resolve( sessionFactory );
			sqlMementoMap.put( namedNativeQueryDefinition.getRegistrationName(), resolved );
			return resolved;
		}
		final NamedProcedureCallDefinition namedCallableQueryDefinition = bootMetamodel.getNamedProcedureCallMapping( registrationName );
		if ( namedCallableQueryDefinition != null ) {
			final NamedCallableQueryMemento resolved = namedCallableQueryDefinition.resolve( sessionFactory );
			callableMementoMap.put( namedCallableQueryDefinition.getRegistrationName(), resolved );
			return resolved;
		}
		return null;
	}

	@Override
	public void prepare(SessionFactoryImplementor sessionFactory, Metadata bootMetamodel) {
		bootMetamodel.visitNamedHqlQueryDefinitions(
				namedHqlQueryDefinition -> {
					final NamedSqmQueryMemento<?> resolved = namedHqlQueryDefinition.resolve( sessionFactory );
					sqmMementoMap.put( namedHqlQueryDefinition.getRegistrationName(), resolved );
				}
		);

		bootMetamodel.visitNamedNativeQueryDefinitions(
				namedNativeQueryDefinition -> {
					final NamedNativeQueryMemento<?> resolved = namedNativeQueryDefinition.resolve( sessionFactory );
					sqlMementoMap.put( namedNativeQueryDefinition.getRegistrationName(), resolved );
				}
		);

		bootMetamodel.visitNamedResultSetMappingDefinition(
				namedResultSetMappingDefinition -> {
					final NamedResultSetMappingMemento resolved = namedResultSetMappingDefinition.resolve( () -> sessionFactory );
					resultSetMappingMementoMap.put( namedResultSetMappingDefinition.getRegistrationName(), resolved );
				}
		);

		bootMetamodel.visitNamedProcedureCallDefinition(
				namedProcedureCallDefinition -> {
					final NamedCallableQueryMemento resolved = namedProcedureCallDefinition.resolve( sessionFactory );
					callableMementoMap.put( namedProcedureCallDefinition.getRegistrationName(), resolved );
				}
		);

	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named query checking

	@Override
	public void validateNamedQueries(QueryEngine queryEngine) {
		final Map<String, HibernateException> errors = checkNamedQueries( queryEngine );
		if ( !errors.isEmpty() ) {
			int i = 0;
			final StringBuilder failingQueries = new StringBuilder( "Errors in named queries: " );
			for ( Map.Entry<String, HibernateException> entry : errors.entrySet() ) {
				QUERY_MESSAGE_LOGGER.namedQueryError( entry.getKey(), entry.getValue() );
				failingQueries.append( "\n" )
						.append("  [").append(++i).append("] Error in query named '").append( entry.getKey() ).append("'")
						.append(": ").append( entry.getValue().getMessage() );
			}
			final NamedQueryValidationException exception =
					new NamedQueryValidationException( failingQueries.toString(), errors );
			errors.values().forEach( exception::addSuppressed );
			throw exception;
		}
	}

	@Override
	public Map<String, HibernateException> checkNamedQueries(QueryEngine queryEngine) {
		Map<String,HibernateException> errors = new HashMap<>();

		final QueryInterpretationCache interpretationCache = queryEngine.getInterpretationCache();

		// Check named HQL queries
		log.debugf( "Checking %s named HQL queries", sqmMementoMap.size() );
		for ( NamedSqmQueryMemento<?> hqlMemento : sqmMementoMap.values() ) {
			final String queryString = hqlMemento.getHqlString();
			final String registrationName = hqlMemento.getRegistrationName();
			try {
				log.debugf( "Checking named HQL query: %s", registrationName );
				interpretationCache.resolveHqlInterpretation(
						queryString,
						null,
						queryEngine.getHqlTranslator()
				);
			}
			catch ( QueryException e ) {
				errors.put( registrationName, e );
			}
			catch ( PathElementException | TerminalPathException e ) {
				errors.put( registrationName, new UnknownPathException( e.getMessage(), queryString, e ) );
			}
			catch ( EntityTypeException e ) {
				errors.put( registrationName, new UnknownEntityException( e.getMessage(), e.getReference(), e ) );
			}
		}

		// Check native-sql queries
		log.debugf( "Checking %s named SQL queries", sqlMementoMap.size() );
		for ( NamedNativeQueryMemento<?> memento : sqlMementoMap.values() ) {
			memento.validate( queryEngine );
//			// this will throw an error if there's something wrong.
//			try {
//				log.debugf( "Checking named SQL query: %s", memento.getRegistrationName() );
//				// TODO : would be really nice to cache the spec on the query-def so as to not have to re-calc the hash;
//				// currently not doable though because of the resultset-ref stuff...
//				NativeSQLQuerySpecification spec;
//				if ( memento.getResultSetMappingName() != null ) {
//					NamedResultSetMappingMemento resultSetMappingMemento = getResultSetMappingMemento( memento.getResultSetMappingName() );
//					if ( resultSetMappingMemento == null ) {
//						throw new MappingException( "Unable to find resultset-ref resultSetMappingMemento: " + memento.getResultSetMappingName() );
//					}
//					spec = new NativeSQLQuerySpecification(
//							namedSQLQueryDefinition.getQueryString(),
//							resultSetMappingMemento.getQueryReturns(),
//							namedSQLQueryDefinition.getQuerySpaces()
//					);
//				}
//				else {
//					spec =  new NativeSQLQuerySpecification(
//							namedSQLQueryDefinition.getQueryString(),
//							namedSQLQueryDefinition.getQueryReturns(),
//							namedSQLQueryDefinition.getQuerySpaces()
//					);
//				}
//				queryEngine.getNativeSQLQueryPlan( spec );
//			}
//			catch ( HibernateException e ) {
//				errors.put( namedSQLQueryDefinition.getName(), e );
//			}
		}

		return errors;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Shutdown

	@Override
	public void close() {
		sqmMementoMap.clear();
		sqlMementoMap.clear();
		callableMementoMap.clear();
		resultSetMappingMementoMap.clear();
	}
}
