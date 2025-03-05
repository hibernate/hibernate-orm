/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.AttributeMapping;

/**
 * Results of analyzing an {@linkplain #getAttribute() attribute} in terms of
 * handling update operations
 *
 * @author Steve Ebersole
 */
@Incubating
public interface AttributeAnalysis {
	/**
	 * The attribute analyzed here
	 */
	AttributeMapping getAttribute();

	/**
	 * Whether the attribute should be included in setting the
	 * values on the database.
	 */
	boolean includeInSet();

	/**
	 * Whether the attribute should be included in
	 * optimistic locking (where-clause restriction)
	 */
	boolean includeInLocking();

	/**
	 * Whether the attribute is considered dirty
	 */
	DirtynessStatus getDirtynessStatus();

	/**
	 * Whether the attribute be skipped completely.
	 *
	 * @see #includeInSet
	 * @see #includeInLocking
	 */
	default boolean isSkipped() {
		return !includeInSet() && !includeInLocking();
	}

	/**
	 * Dirty-ness status of each attribute:
	 * it's useful to differentiate when it's definitely dirty,
	 * when it's definitely not dirty, and when we need to treat
	 * it like dirty but there is no certainty - for example
	 * because we didn't actually load the value from the database.
	 */
	enum DirtynessStatus {
		DIRTY,
		NOT_DIRTY {
			@Override
			boolean isDirty() {
				return false;
			}
		},
		CONSIDER_LIKE_DIRTY;

		/**
		 * @return both DIRTY and CONSIDER_LIKE_DIRTY states will return {@code true}
		 */
		boolean isDirty() {
			return true;
		}
	}

}
