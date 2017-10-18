/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.type.ForeignKeyDirection;

/**
 * Specialization for Navigable that can be used in creating TableGroupJoins
 *
 * @author Steve Ebersole
 */
public interface Joinable<T> extends Navigable<T> {
	/**
	 * Intended for metadata-tive purposes.  Internally Hibernate never uses this
	 * method, since the specific Joinable Navigables simply incorporate these
	 * into their corresponding TableGroupJoin#predicate and QueryResult.
	 * <p/>
	 * Can be `null` when the joinable is an embedded value.
	 */
	ForeignKey.ColumnMappings getJoinColumnMappings();

	default ForeignKeyDirection getForeignKeyDirection() {
		throw new NotYetImplementedFor6Exception();
	}

	default boolean isCascadeDeleteEnabled() {
		throw new NotYetImplementedFor6Exception();
	}
}
