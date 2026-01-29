/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;

import jakarta.persistence.Timeout;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.spi.JpaReference;

import java.util.Map;

/**
 * Boot-model representation of named queries.
 * <p>
 * Ultimately this is used to {@linkplain #resolve make} a
 * named query memento.
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
public interface NamedQueryDefinition<T> extends JpaReference {
	/**
	 * The name under which the query is to be registered.
	 */
	String getRegistrationName();

	@Override
	default String getName() {
		return getRegistrationName();
	}

	FlushMode getQueryFlushMode();

	Timeout getTimeout();

	String getComment();

	@Override
	Map<String, Object> getHints();

	/**
	 * The location at which the defining named query annotation occurs,
	 * usually a class or package name. Null for named queries declared
	 * in XML.
	 */
	@Nullable String getLocation();

	/**
	 * Resolve the mapping definition into its run-time memento form.
	 */
	NamedQueryMemento<T> resolve(SessionFactoryImplementor factory);
}
