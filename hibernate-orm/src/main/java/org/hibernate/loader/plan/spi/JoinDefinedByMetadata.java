/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.spi;

import org.hibernate.type.Type;

/**
 * Specialization of a Join that is defined by the metadata.
 *
 * @author Steve Ebersole
 */
public interface JoinDefinedByMetadata extends Join {
	/**
	 * Obtain the name of the property that defines the join, relative to the PropertyMapping
	 * ({@link QuerySpace#toAliasedColumns(String, String)}) of the left-hand-side
	 * ({@link #getLeftHandSide()}) of the join
	 *
	 * @return The property name
	 */
	public String getJoinedPropertyName();

	/**
	 * Get the property type of the joined property.
	 *
	 * @return The property type.
	 */
	public Type getJoinedPropertyType();
}
