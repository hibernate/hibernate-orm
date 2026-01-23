/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.internal;

import jakarta.persistence.Statement;
import jakarta.persistence.StatementReference;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.HibernateException;
import org.hibernate.QueryException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.query.NamedQueryValidationException;
import org.hibernate.query.UnknownNamedQueryException;
import org.hibernate.query.named.NamedMutationMemento;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.named.NamedSelectionMemento;
import org.hibernate.query.named.StatementReferenceProducer;
import org.hibernate.query.named.TypedQueryReferenceProducer;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.named.NamedNativeQueryMemento;
import org.hibernate.query.sqm.EntityTypeException;
import org.hibernate.query.sqm.PathElementException;
import org.hibernate.query.sqm.TerminalPathException;
import org.hibernate.query.sqm.UnknownEntityException;
import org.hibernate.query.sqm.UnknownPathException;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;
import static org.hibernate.query.QueryLogging.QUERY_MESSAGE_LOGGER;

/**
 * @author Steve Ebersole
 */
public class NamedObjectRepositoryImpl implements NamedObjectRepository {
	private static final Logger LOG = Logger.getLogger( NamedObjectRepository.class );

	private final Map<String, NamedSelectionMemento<?>> selectionMementos;
	private final Map<String, NamedMutationMemento<?>> mutationMementos;
	private final Map<String, NamedCallableQueryMemento> callableMementoMap;

	private final Map<String, NamedResultSetMappingMemento> resultSetMappingMementoMap;

	public NamedObjectRepositoryImpl(
			Map<String,NamedSelectionMemento<?>> selectionMementos,
			Map<String,NamedMutationMemento<?>> mutationMementos,
			Map<String,NamedCallableQueryMemento> callableMementoMap,
			Map<String,NamedResultSetMappingMemento> resultSetMappingMementoMap) {
		this.selectionMementos = selectionMementos;
		this.mutationMementos = mutationMementos;
		this.callableMementoMap = callableMementoMap;
		this.resultSetMappingMementoMap = resultSetMappingMementoMap;
	}

	@Override
	public <R> NamedQueryMemento<R> findQueryMementoByName(String name, boolean includeProcedureCalls) {
		NamedQueryMemento<?> match = selectionMementos.get( name );
		if ( match == null ) {
			match = mutationMementos.get( name );
		}
		if ( includeProcedureCalls && match == null ) {
			match = callableMementoMap.get( name );
		}
		//noinspection unchecked
		return (NamedQueryMemento<R>) match;
	}

	@Override
	public <R> @NonNull NamedQueryMemento<R> getQueryMementoByName(String name, boolean includeProcedureCalls) {
		final var match = findQueryMementoByName( name, includeProcedureCalls );
		if ( match == null ) {
			throw new UnknownNamedQueryException( name );
		}
		//noinspection unchecked
		return (NamedQueryMemento<R>) match;
	}

	@Override
	public <R> NamedSelectionMemento<R> getSelectionQueryMemento(String name) {
		//noinspection unchecked
		return (NamedSelectionMemento<R>) selectionMementos.get( name );
	}

	@Override
	public <R> NamedMutationMemento<R> getMutationQueryMemento(String name) {
		//noinspection unchecked
		return (NamedMutationMemento<R>) mutationMementos.get( name );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> Map<String, TypedQueryReference<R>> getNamedQueries(Class<R> resultType) {
		final Map<String, TypedQueryReference<R>> matches = mapOfSize( selectionMementos.size() );
		for ( Map.Entry<String, NamedSelectionMemento<?>> entry : selectionMementos.entrySet() ) {
			if ( resultType.equals( entry.getValue().getResultType() ) ) {
				matches.put( entry.getKey(), (NamedSelectionMemento<R>) entry.getValue() );
			}
		}
		return matches;
	}

	@Override
	public void forEachNamedQuery(BiConsumer<String,? super TypedQueryReference<?>> action) {
		selectionMementos.forEach( action );
	}

	@Override
	public <R> TypedQueryReference<R> registerNamedQuery(String name, TypedQuery<R> query) {
		try {
			var refProducer = query.unwrap( TypedQueryReferenceProducer.class );
			var ref = refProducer.toSelectionMemento( name );
			selectionMementos.put( name, ref );
			//noinspection unchecked
			return (TypedQueryReference<R>) ref;
		}
		catch (Exception e) {
			throw new IllegalArgumentException( "Could not create TypedQueryReference from given TypedQuery - " + query, e );
		}
	}

	@Override
	public Map<String, StatementReference> getNamedMutations() {
		return Collections.unmodifiableMap( mutationMementos );
	}

	@Override
	public @NonNull StatementReference registerNamedMutation(String name, Statement statement) {
		try {
			var refProducer = statement.unwrap( StatementReferenceProducer.class );
			var ref = refProducer.toMutationMemento( name );
			mutationMementos.put( name, ref );
			return ref;
		}
		catch (Exception e) {
			throw new IllegalArgumentException( "Could not create StatementReference from given Statement - " + statement, e );
		}
	}

	@Override
	public void forEachNamedMutation(BiConsumer<String,? super StatementReference> action) {
		mutationMementos.forEach( action );
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
		NamedQueryMemento<?> namedQuery = selectionMementos.get( registrationName );
		if ( namedQuery != null ) {
			return namedQuery;
		}
		namedQuery = mutationMementos.get( registrationName );
		if ( namedQuery != null ) {
			return namedQuery;
		}
		namedQuery = callableMementoMap.get( registrationName );
		if ( namedQuery != null ) {
			return namedQuery;
		}

		final var namedHqlQueryDefinition = bootMetamodel.getNamedHqlQueryMapping( registrationName );
		if ( namedHqlQueryDefinition != null ) {
			return handleNamedHqlDefinition( namedHqlQueryDefinition, sessionFactory );
		}

		final var namedNativeQueryDefinition = bootMetamodel.getNamedNativeQueryMapping( registrationName );
		if ( namedNativeQueryDefinition != null ) {
			return handleNamedNativeDefinition( namedNativeQueryDefinition, sessionFactory );
		}
		final var namedCallableQueryDefinition = bootMetamodel.getNamedProcedureCallMapping( registrationName );
		if ( namedCallableQueryDefinition != null ) {
			final var memento = namedCallableQueryDefinition.resolve( sessionFactory );
			callableMementoMap.put( namedCallableQueryDefinition.getRegistrationName(), memento );
			return memento;
		}
		return null;
	}

	private NamedQueryMemento<?> handleNamedHqlDefinition(NamedHqlQueryDefinition<?> namedHqlQueryDefinition, SessionFactoryImplementor sessionFactory) {
		var memento = namedHqlQueryDefinition.resolve( sessionFactory );
		if ( memento instanceof NamedSelectionMemento<?> selectionMemento ) {
			selectionMementos.put( namedHqlQueryDefinition.getName(), selectionMemento );
		}
		else {
			mutationMementos.put( namedHqlQueryDefinition.getName(), (NamedMutationMemento<?>) memento );
		}
		return memento;
	}

	private NamedNativeQueryMemento<?> handleNamedNativeDefinition(NamedNativeQueryDefinition<?> namedNativeQueryDefinition, SessionFactoryImplementor sessionFactory) {
		final var memento = namedNativeQueryDefinition.resolve( sessionFactory );
		if ( memento instanceof NamedSelectionMemento<?> selectionMemento ) {
			selectionMementos.put( namedNativeQueryDefinition.getName(), selectionMemento );
		}
		else {
			mutationMementos.put( namedNativeQueryDefinition.getName(), (NamedMutationMemento<?>) memento );
		}
		return memento;
	}

	@Override
	public void prepare(SessionFactoryImplementor sessionFactory, Metadata bootMetamodel) {
		bootMetamodel.visitNamedHqlQueryDefinitions( definition ->
				handleNamedHqlDefinition( definition, sessionFactory )
		);

		bootMetamodel.visitNamedNativeQueryDefinitions( definition ->
				handleNamedNativeDefinition( definition, sessionFactory )
		);

		bootMetamodel.visitNamedResultSetMappingDefinition(
				namedResultSetMappingDefinition ->
						resultSetMappingMementoMap.put( namedResultSetMappingDefinition.getRegistrationName(),
								namedResultSetMappingDefinition.resolve( () -> sessionFactory ) )
		);

		bootMetamodel.visitNamedProcedureCallDefinition(
				namedProcedureCallDefinition ->
						callableMementoMap.put( namedProcedureCallDefinition.getRegistrationName(),
								namedProcedureCallDefinition.resolve( sessionFactory ) )
		);

	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named query checking

	@Override
	public void validateNamedQueries(QueryEngine queryEngine) {
		final var errors = checkNamedQueries( queryEngine );
		if ( !errors.isEmpty() ) {
			int i = 0;
			final StringBuilder failingQueries = new StringBuilder( "Errors in named queries: " );
			for ( var entry : errors.entrySet() ) {
				QUERY_MESSAGE_LOGGER.namedQueryError( entry.getKey(), entry.getValue() );
				failingQueries.append( "\n" )
						.append("  [").append(++i).append("] Error in query named '").append( entry.getKey() ).append("'")
						.append(": ").append( entry.getValue().getMessage() );
			}
			final var exception = new NamedQueryValidationException( failingQueries.toString(), errors );
			errors.values().forEach( exception::addSuppressed );
			throw exception;
		}
	}

	@Override
	public Map<String, HibernateException> checkNamedQueries(QueryEngine queryEngine) {
		final Map<String,HibernateException> errors = new HashMap<>();

		LOG.tracef( "Checking %s named selection queries", selectionMementos.size() );
		for ( var memento : selectionMementos.values() ) {
			final String queryString = memento.getSelectionString();
			final String registrationName = memento.getRegistrationName();
			try {
				LOG.tracef( "Checking named selection query: %s", registrationName );
				memento.validate( queryEngine );
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

		// Check mutation queries
		LOG.tracef( "Checking %s named mutation queries", mutationMementos.size() );
		for ( var memento : mutationMementos.values() ) {
			final String registrationName = memento.getRegistrationName();
			LOG.tracef( "Checking named mutation query: %s", registrationName );
			memento.validate( queryEngine );
		}

		return errors;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Shutdown

	@Override
	public void close() {
		selectionMementos.clear();
		mutationMementos.clear();
		callableMementoMap.clear();
		resultSetMappingMementoMap.clear();
	}
}
