/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
	boolean isDirty();

	/**
	 * Whether the attribute be skipped completely.
	 *
	 * @see #includeInSet
	 * @see #includeInLocking
	 */
	default boolean isSkipped() {
		return !includeInSet() && !includeInLocking();
	}
}
