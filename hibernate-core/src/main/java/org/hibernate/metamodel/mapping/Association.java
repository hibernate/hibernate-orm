/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.sql.results.graph.Fetchable;

/**
 * Commonality for an association, mainly details relative to the foreign-key
 *
 * @author Steve Ebersole
 */
public interface Association extends Fetchable {
	/**
	 * The descriptor, allowing access to column(s), etc
	 */
	ForeignKeyDescriptor getForeignKeyDescriptor();

	/**
	 * Indicates which "side" of the foreign-key this association describes
	 */
	ForeignKeyDescriptor.Nature getSideNature();
}
