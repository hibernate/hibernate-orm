/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named;

import jakarta.persistence.Timeout;
import org.hibernate.FlushMode;
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
	String getRegistrationName();

	FlushMode getFlushMode();

	Timeout getTimeout();

	String getComment();

	Map<String, Object> getHints();

	void validate(QueryEngine queryEngine);

	/**
	 * Makes a copy of the memento using the specified registration name
	 */
	NamedQueryMemento<T> makeCopy(String name);

	/// Create a [selection queries][SelectionQueryImplementor] based on this reference's definition.
	SelectionQueryImplementor<T> toSelectionQuery(SharedSessionContractImplementor session);

	/// Create a [selection queries][SelectionQueryImplementor] based on this reference's definition with the give result type.
	<X> SelectionQueryImplementor<X> toSelectionQuery(SharedSessionContractImplementor session, Class<X> javaType);

	/// Create a [mutation queries][MutationQueryImplementor] based on this memento's definition.
	MutationQueryImplementor<T> toMutationQuery(SharedSessionContractImplementor session);

	/// Create a [mutation queries][MutationQueryImplementor] based on this memento's definition.
	<X> MutationQueryImplementor<X> toMutationQuery(SharedSessionContractImplementor session, Class<X> targetType);

	/// Create a [QueryImplementor] reference.  Used in cases where we do not know
	/// up front if we have a selection or mutation query.
	///
	/// @see #toSelectionQuery
	/// @see #toMutationQuery
	QueryImplementor<T> toQuery(SharedSessionContractImplementor session);

	/// Create a [QueryImplementor] reference.  Used in cases where we do not know
	/// up front if we have a selection or mutation query.
	///
	/// @see #toSelectionQuery
	/// @see #toMutationQuery
	<X> QueryImplementor<X> toQuery(SharedSessionContractImplementor session, Class<X> javaType);

	interface ParameterMemento {
		QueryParameterImplementor<?> resolve(SharedSessionContractImplementor session);
	}
}
