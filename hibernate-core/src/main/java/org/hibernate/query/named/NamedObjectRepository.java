/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Statement;
import jakarta.persistence.StatementReference;
import jakarta.persistence.TypedQuery;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.query.UnknownNamedQueryException;
import org.hibernate.query.spi.QueryEngine;

import jakarta.persistence.TypedQueryReference;

/**
 * Repository for references to named things related to queries. This includes:
 * <ul>
 * <li>named HQL, JPQL, native, and procedure queries,
 * <li>along with named result set mappings.
 * </ul>
 *
 * @author Steve Ebersole
 */
@Incubating
public interface NamedObjectRepository {

	/**
	 * Returns all selection-query references keyed by registration name.
	 *
	 * @see jakarta.persistence.EntityManagerFactory#getNamedQueries(Class)
	 * @see jakarta.persistence.NamedQuery
	 * @see jakarta.persistence.NamedNativeQuery
	 * @see #registerNamedQuery
	 * @see org.hibernate.SessionFactory#addNamedQuery(String, TypedQuery)
	 * @see org.hibernate.query.SelectionQuery
	 */
	<R> Map<String, TypedQueryReference<R>> getNamedQueries(Class<R> resultType);

	/**
	 * Perform an action for all registered selection-query references.
	 */
	void forEachNamedQuery(BiConsumer<String,? super TypedQueryReference<?>> action);

	/**
	 * Registers the given selection-query using the given name,
	 * returning a {@linkplain TypedQueryReference registration reference},
	 * which can be used later to create a {@linkplain org.hibernate.query.SelectionQuery query}
	 * instance.
	 *
	 * @see #getNamedQueries(Class)
	 * @see org.hibernate.SessionFactory#addNamedQuery(String, TypedQuery)
	 * @see org.hibernate.Session#createQuery(TypedQueryReference)
	 */
	<R> TypedQueryReference<R> registerNamedQuery(String name, TypedQuery<R> query);

	/**
	 * Returns all mutation-query references keyed by registration name.
	 *
	 * @see EntityManagerFactory#getNamedStatements()
	 * @see jakarta.persistence.NamedStatement
	 * @see jakarta.persistence.NamedNativeStatement
	 * @see #registerNamedMutation
	 * @see org.hibernate.SessionFactory#addNamedStatement(String, Statement)
	 * @see org.hibernate.query.MutationQuery
	 */
	Map<String, StatementReference> getNamedMutations();

	/**
	 * Perform an action for all registered mutation-query references.
	 */
	void forEachNamedMutation(BiConsumer<String,? super StatementReference> action);

	/**
	 * Registers the given mutation-query using the given name, returning
	 * a {@linkplain StatementReference registration reference}, which can
	 * be used later to create a {@linkplain org.hibernate.query.MutationQuery query}
	 * instance.
	 *
	 * @see #getNamedMutations
	 * @see org.hibernate.SessionFactory#addNamedStatement(String, Statement)
	 * @see org.hibernate.Session#createStatement(StatementReference)
	 */
	@NonNull StatementReference registerNamedMutation(String name, Statement statement);

	/**
	 * Find a query registration by name, regardless of query type.
	 *
	 * @return The query registration, or {@code null} if one could not be found
	 */
	<R> @Nullable NamedQueryMemento<R> findQueryMementoByName(String name, boolean includeProcedureCalls);

	/**
	 * Find a query registration by name, regardless of query type, throwing
	 * an exception if one could not be found.
	 *
	 * @return The query registration.
	 *
	 * @throws UnknownNamedQueryException If one could not be found under that name.
	 */
	<R> @NonNull NamedQueryMemento<R> getQueryMementoByName(String name, boolean includeProcedureCalls);

	<R> NamedSelectionMemento<R> getSelectionQueryMemento(String name);
	<R> NamedMutationMemento<R> getMutationQueryMemento(String name);

	NamedCallableQueryMemento getCallableQueryMemento(String name);
	void visitCallableQueryMementos(Consumer<NamedCallableQueryMemento> action);
	void registerCallableQueryMemento(String name, NamedCallableQueryMemento memento);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named ResultSetMapping memento

	NamedResultSetMappingMemento getResultSetMappingMemento(String mappingName);
	void visitResultSetMappingMementos(Consumer<NamedResultSetMappingMemento> action);
	void registerResultSetMappingMemento(String name, NamedResultSetMappingMemento memento);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// boot-time

	/**
	 * Perform a validity check on all named queries
	 */
	Map<String, HibernateException> checkNamedQueries(QueryEngine queryPlanCache);

	/**
	 * Validate the named queries and throw an exception if any are broken
	 */
	void validateNamedQueries(QueryEngine queryEngine);

	/**
	 * Resolve the named query with the given name.
	 */
	NamedQueryMemento<?> resolve(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor bootMetamodel,
			String registrationName);

	/**
	 * Prepare for runtime use
	 */
	// TODO: avoid passing in the whole SessionFactory here, it's not necessary
	void prepare(SessionFactoryImplementor sessionFactory, Metadata bootMetamodel);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Shutdown

	/**
	 * Release any held resources
	 */
	void close();
}
