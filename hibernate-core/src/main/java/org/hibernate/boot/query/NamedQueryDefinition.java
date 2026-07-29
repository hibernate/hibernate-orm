/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;

import java.io.Serializable;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Timeout;
import jakarta.annotation.Nullable;
import org.hibernate.query.spi.JpaReference;

/**
 * Boot-model representation of named queries.
 * <p>
 * Ultimately this is used to make a named query memento.
 *
 * @see org.hibernate.annotations.NamedQuery
 * @see org.hibernate.annotations.NamedNativeQuery
 * @see jakarta.persistence.NamedQuery
 * @see jakarta.persistence.NamedNativeQuery
 * @see jakarta.persistence.NamedStoredProcedureQuery
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface NamedQueryDefinition<T> extends JpaReference, Serializable {
	/**
	 * The name under which the query is to be registered.
	 */
	@Nonnull
	String getRegistrationName();

	@Nonnull
	@Override
	default String getName() {
		return getRegistrationName();
	}

	@Nonnull
	QueryFlushMode getQueryFlushMode();

	@Nullable
	Timeout getTimeout();

	@Nullable
	String getComment();

	@Nonnull
	@Override
	Map<String, Object> getHints();

	/**
	 * The location at which the defining named query annotation occurs,
	 * usually a class or package name. Null for named queries declared
	 * in XML.
	 */
	@Nullable
	String getLocation();

}
