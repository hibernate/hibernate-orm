/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import org.hibernate.Incubating;

/**
 * Represents a parameter defined in the source (HQL/JPQL or criteria) query.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface QueryParameter<T> extends jakarta.persistence.Parameter<T> {
	/**
	 * Does this parameter allow multi-valued (collection, array, etc) binding?
	 * <p>
	 * This is only valid for HQL/JPQL and (I think) Criteria queries, and is
	 * determined based on the context of the parameters declaration.
	 *
	 * @return {@code true} indicates that multi-valued binding is allowed for this
	 * parameter
	 */
	boolean allowsMultiValuedBinding();

	/**
	 * Get the Hibernate Type associated with this parameter, if one.  May
	 * return {@code null}.
	 *
	 * @return The associated Hibernate Type, may be {@code null}.
	 */
	BindableType<T> getHibernateType();
}
