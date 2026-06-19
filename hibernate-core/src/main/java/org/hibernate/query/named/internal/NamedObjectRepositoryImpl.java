/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Statement;
import jakarta.persistence.StatementReference;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
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
import org.hibernate.query.named.spi.NamedMutationMemento;
import org.hibernate.query.named.spi.NamedObjectRepository;
import org.hibernate.query.named.spi.NamedQueryMemento;
import org.hibernate.query.named.spi.NamedResultSetMappingMemento;
import org.hibernate.query.named.spi.NamedSelectionMemento;
import org.hibernate.query.named.spi.StatementReferenceProducer;
import org.hibernate.query.named.spi.TypedQueryReferenceProducer;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.named.spi.NamedNativeQueryMemento;
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
import static org.hibernate.query.internal.QueryLogging.QUERY_MESSAGE_LOGGER;

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
			@Nonnull Map<String,NamedSelectionMemento<?>> selectionMementos,
			@Nonnull Map<String,NamedMutationMemento<?>> mutationMementos,
			@Nonnull Map<String,NamedCallableQueryMemento> callableMementoMap,
			@Nonnull Map<String,NamedResultSetMappingMemento> resultSetMappingMementoMap) {
		this.selectionMementos = selectionMementos;
		this.mutationMementos = mutationMementos;
		this.callableMementoMap = callableMementoMap;
		this.resultSetMappingMementoMap = resultSetMappingMementoMap;
	}

	@Override
	@Nullable
	public <R> NamedQueryMemento<R> findQueryMementoByName(@Nonnull String name, boolean includeProcedureCalls) {
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
	@Nonnull
	public <R> NamedQueryMemento<R> getQueryMementoByName(@Nonnull String name, boolean includeProcedureCalls) {
		final var match = findQueryMementoByName( name, includeProcedureCalls );
		if ( match == null ) {
			throw new UnknownNamedQueryException( name );
		}
		//noinspection unchecked
		return (NamedQueryMemento<R>) match;
	}

	@Override
	@Nullable
	public <R> NamedSelectionMemento<R> getSelectionQueryMemento(@Nonnull String name) {
		//noinspection unchecked
		return (NamedSelectionMemento<R>) selectionMementos.get( name );
	}

	@Override
	@Nullable
	public <R> NamedMutationMemento<R> getMutationQueryMemento(@Nonnull String name) {
		//noinspection unchecked
		return (NamedMutationMemento<R>) mutationMementos.get( name );
	}

	@Override
	@Nonnull
	@SuppressWarnings("unchecked")
	public <R> Map<String, TypedQueryReference<R>> getNamedQueries(@Nonnull Class<R> resultType) {
		final Map<String, TypedQueryReference<R>> matches = mapOfSize( selectionMementos.size() );
		for ( var entry : selectionMementos.entrySet() ) {
			if ( resultType.equals( entry.getValue().getResultType() ) ) {
				matches.put( entry.getKey(), (NamedSelectionMemento<R>) entry.getValue() );
			}
		}
		return matches;
	}

	@Override
	public void forEachNamedQuery(@Nonnull BiConsumer<String,? super TypedQueryReference<?>> action) {
		selectionMementos.forEach( action );
	}

	@Override
	@Nonnull
	public <R> TypedQueryReference<R> registerNamedQuery(@Nonnull String name, @Nonnull TypedQuery<R> query) {
		try {
			final var refProducer = query.unwrap( TypedQueryReferenceProducer.class );
			final var ref = refProducer.toSelectionMemento( name );
			selectionMementos.put( name, ref );
			//noinspection unchecked
			return (TypedQueryReference<R>) ref;
		}
		catch (Exception e) {
			throw new IllegalArgumentException( "Could not create TypedQueryReference from given TypedQuery - " + query, e );
		}
	}

	@Override
	@Nonnull
	public Map<String, StatementReference> getNamedMutations() {
		return Collections.unmodifiableMap( mutationMementos );
	}

	@Override
	@Nonnull
	public StatementReference registerNamedMutation(@Nonnull String name, @Nonnull Statement statement) {
		try {
			final var refProducer = statement.unwrap( StatementReferenceProducer.class );
			final var ref = refProducer.toMutationMemento( name );
			mutationMementos.put( name, ref );
			return ref;
		}
		catch (Exception e) {
			throw new IllegalArgumentException( "Could not create StatementReference from given Statement - " + statement, e );
		}
	}

	@Override
	public void forEachNamedMutation(@Nonnull BiConsumer<String,? super StatementReference> action) {
		mutationMementos.forEach( action );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// callable mementos

	@Override
	@Nullable
	public NamedCallableQueryMemento getCallableQueryMemento(@Nonnull String name) {
		return callableMementoMap.get( name );
	}

	@Override
	public void visitCallableQueryMementos(@Nonnull Consumer<NamedCallableQueryMemento> action) {
		callableMementoMap.values().forEach( action );
	}

	@Override
	public synchronized void registerCallableQueryMemento(
			@Nonnull String name,
			@Nonnull NamedCallableQueryMemento memento) {
		callableMementoMap.put( name, memento );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Result-set mapping mementos

	@Override
	@Nullable
	public NamedResultSetMappingMemento getResultSetMappingMemento(@Nonnull String mappingName) {
		return resultSetMappingMementoMap.get( mappingName );
	}

	@Override
	public void visitResultSetMappingMementos(@Nonnull Consumer<NamedResultSetMappingMemento> action) {
		resultSetMappingMementoMap.values().forEach( action );
	}

	@Override
	public void registerResultSetMappingMemento(
			@Nonnull String name,
			@Nonnull NamedResultSetMappingMemento memento) {
		resultSetMappingMementoMap.put( name, memento );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Prepare repository for use

	@Override
	@Nullable
	public NamedQueryMemento<?> resolve(
			@Nonnull SessionFactoryImplementor sessionFactory,
			@Nonnull MetadataImplementor bootMetamodel,
			@Nonnull String registrationName) {
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

	@Nonnull
	private NamedQueryMemento<?> handleNamedHqlDefinition(
			@Nonnull NamedHqlQueryDefinition<?> namedHqlQueryDefinition,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		var memento = namedHqlQueryDefinition.resolve( sessionFactory );
		if ( memento instanceof NamedSelectionMemento<?> selectionMemento ) {
			selectionMementos.put( namedHqlQueryDefinition.getName(), selectionMemento );
		}
		else {
			mutationMementos.put( namedHqlQueryDefinition.getName(), (NamedMutationMemento<?>) memento );
		}
		return memento;
	}

	@Nonnull
	private NamedNativeQueryMemento<?> handleNamedNativeDefinition(
			@Nonnull NamedNativeQueryDefinition<?> namedNativeQueryDefinition,
			@Nonnull SessionFactoryImplementor sessionFactory) {
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
	public void prepare(@Nonnull SessionFactoryImplementor sessionFactory, @Nonnull Metadata bootMetamodel) {
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
	public void validateNamedQueries(@Nonnull QueryEngine queryEngine) {
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
	@Nonnull
	public Map<String, HibernateException> checkNamedQueries(@Nonnull QueryEngine queryEngine) {
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
