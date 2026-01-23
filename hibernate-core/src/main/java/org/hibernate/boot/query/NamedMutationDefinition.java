/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.spi.JpaStatementReference;

/**
 * Used to model named mutation-query attributes.
 *
 * @see jakarta.persistence.NamedStatement
 * @see jakarta.persistence.NamedNativeStatement
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface NamedMutationDefinition<T>
		extends NamedQueryDefinition<T>, JpaStatementReference<T> {
	@Override
	default String getName() {
		return getRegistrationName();
	}

	String getStatementString();

	/**
	 * The name under which the query is to be registered.
	 */
	String getRegistrationName();

	/**
	 * The location at which the defining named query annotation occurs,
	 * usually a class or package name. Null for named queries declared
	 * in XML.
	 */
	@Nullable
	String getLocation();

	/**
	 * Resolve the mapping definition into its run-time memento form.
	 */
	NamedQueryMemento<T> resolve(SessionFactoryImplementor factory);
}
