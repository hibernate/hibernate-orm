/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Timeout;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.JpaReference;
import org.hibernate.query.spi.MutationQueryImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.SelectionQueryImplementor;

import java.util.Map;

/// The runtime representation of named queries.  They are stored in and
/// available through the QueryEngine's [NamedObjectRepository].
/// This is the base contract for all specific types of named query mementos
///
/// @author Steve Ebersole
public interface NamedQueryMemento<T> extends JpaReference {
	/**
	 * The name under which the query is registered
	 */
	@Nonnull
	String getRegistrationName();

	@Nullable
	QueryFlushMode getQueryFlushMode();

	@Nullable
	Timeout getTimeout();

	@Nullable
	String getComment();

	@Nonnull
	Map<String, Object> getHints();

	void validate(@Nonnull QueryEngine queryEngine);

	/**
	 * Makes a copy of the memento using the specified registration name
	 */
	@Nonnull
	NamedQueryMemento<T> makeCopy(@Nonnull String name);

	/// Create a [selection queries][SelectionQueryImplementor] based on this reference's definition.
	@Nonnull
	SelectionQueryImplementor<T> toSelectionQuery(@Nonnull SharedSessionContractImplementor session);

	/// Create a [selection queries][SelectionQueryImplementor] based on this reference's definition with the give result type.
	@Nonnull
	<X> SelectionQueryImplementor<X> toSelectionQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<X> javaType);

	/// Create a [mutation queries][MutationQueryImplementor] based on this memento's definition.
	@Nonnull
	MutationQueryImplementor<T> toMutationQuery(@Nonnull SharedSessionContractImplementor session);

	/// Create a [mutation queries][MutationQueryImplementor] based on this memento's definition.
	@Nonnull
	<X> MutationQueryImplementor<X> toMutationQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<X> targetType);

	/// Create a [QueryImplementor] reference.  Used in cases where we do not know
	/// up front if we have a selection or mutation query.
	///
	/// @see #toSelectionQuery
	/// @see #toMutationQuery
	@Nonnull
	QueryImplementor<T> toQuery(@Nonnull SharedSessionContractImplementor session);

	/// Create a [QueryImplementor] reference.  Used in cases where we do not know
	/// up front if we have a selection or mutation query.
	///
	/// @see #toSelectionQuery
	/// @see #toMutationQuery
	@Nonnull
	<X> QueryImplementor<X> toQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<X> javaType);

	interface ParameterMemento {
		@Nonnull
		QueryParameterImplementor<?> resolve(@Nonnull SharedSessionContractImplementor session);
	}
}
