/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;

import jakarta.persistence.TypedQueryReference;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.NamedQueryMemento;

import org.checkerframework.checker.nullness.qual.Nullable;


/**
 * Common attributes shared across the mapping of named HQL, native
 * and "callable" queries defined in annotations, orm.xml and hbm.xml
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface NamedQueryDefinition<E> extends TypedQueryReference<E> {
	@Override
	default String getName() {
		return getRegistrationName();
	}

	/**
	 * The name under which the query is to be registered.
	 */
	String getRegistrationName();

	/**
	 * The expected result type of the query, or {@code null}.
	 */
	@Nullable
	Class<E> getResultType();

	/**
	 * Resolve the mapping definition into its run-time memento form.
	 */
	NamedQueryMemento<E> resolve(SessionFactoryImplementor factory);

	/**
	 * The location at which the defining named query annotation occurs,
	 * usually a class or package name. Null for named queries declared
	 * in XML.
	 */
	@Nullable String getLocation();
}
