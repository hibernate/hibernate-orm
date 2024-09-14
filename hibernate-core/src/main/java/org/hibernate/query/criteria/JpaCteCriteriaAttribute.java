/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;

/**
 * Describes the attribute of a {@link JpaCteCriteriaType}.
 */
@Incubating
public interface JpaCteCriteriaAttribute extends JpaCriteriaNode {

	/**
	 * The declaring type.
	 */
	JpaCteCriteriaType<?> getDeclaringType();

	/**
	 * The name of the attribute.
	 */
	String getName();

	/**
	 * The java type of the attribute.
	 */
	Class<?> getJavaType();
}
