/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.spi;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Statement;
import jakarta.persistence.StatementReference;
import jakarta.persistence.TypedQuery;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
	@Nonnull
	<R> Map<String, TypedQueryReference<R>> getNamedQueries(@Nonnull Class<R> resultType);

	/**
	 * Perform an action for all registered selection-query references.
	 */
	void forEachNamedQuery(@Nonnull BiConsumer<String,? super TypedQueryReference<?>> action);

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
	@Nonnull
	<R> TypedQueryReference<R> registerNamedQuery(@Nonnull String name, @Nonnull TypedQuery<R> query);

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
	@Nonnull
	Map<String, StatementReference> getNamedMutations();

	/**
	 * Perform an action for all registered mutation-query references.
	 */
	void forEachNamedMutation(@Nonnull BiConsumer<String,? super StatementReference> action);

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
	@Nonnull
	StatementReference registerNamedMutation(@Nonnull String name, @Nonnull Statement statement);

	/**
	 * Find a query registration by name, regardless of query type.
	 *
	 * @return The query registration, or {@code null} if one could not be found
	 */
	@Nullable
	<R> NamedQueryMemento<R> findQueryMementoByName(@Nonnull String name, boolean includeProcedureCalls);

	/**
	 * Find a query registration by name, regardless of query type, throwing
	 * an exception if one could not be found.
	 *
	 * @return The query registration.
	 *
	 * @throws UnknownNamedQueryException If one could not be found under that name.
	 */
	@Nonnull
	<R> NamedQueryMemento<R> getQueryMementoByName(@Nonnull String name, boolean includeProcedureCalls);

	@Nullable
	<R> NamedSelectionMemento<R> getSelectionQueryMemento(@Nonnull String name);

	@Nullable
	<R> NamedMutationMemento<R> getMutationQueryMemento(@Nonnull String name);

	@Nullable
	NamedCallableQueryMemento getCallableQueryMemento(@Nonnull String name);

	void visitCallableQueryMementos(@Nonnull Consumer<NamedCallableQueryMemento> action);

	void registerCallableQueryMemento(@Nonnull String name, @Nonnull NamedCallableQueryMemento memento);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named ResultSetMapping memento

	@Nullable
	NamedResultSetMappingMemento getResultSetMappingMemento(@Nonnull String mappingName);

	void visitResultSetMappingMementos(@Nonnull Consumer<NamedResultSetMappingMemento> action);

	void registerResultSetMappingMemento(@Nonnull String name, @Nonnull NamedResultSetMappingMemento memento);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// boot-time

	/**
	 * Perform a validity check on all named queries
	 */
	@Nonnull
	Map<String, HibernateException> checkNamedQueries(@Nonnull QueryEngine queryPlanCache);

	/**
	 * Validate the named queries and throw an exception if any are broken
	 */
	void validateNamedQueries(@Nonnull QueryEngine queryEngine);

	/**
	 * Resolve the named query with the given name.
	 */
	@Nullable
	NamedQueryMemento<?> resolve(
			@Nonnull SessionFactoryImplementor sessionFactory,
			@Nonnull MetadataImplementor bootMetamodel,
			@Nonnull String registrationName);

	/**
	 * Prepare for runtime use
	 */
	// TODO: avoid passing in the whole SessionFactory here, it's not necessary
	void prepare(@Nonnull SessionFactoryImplementor sessionFactory, @Nonnull Metadata bootMetamodel);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Shutdown

	/**
	 * Release any held resources
	 */
	void close();
}
