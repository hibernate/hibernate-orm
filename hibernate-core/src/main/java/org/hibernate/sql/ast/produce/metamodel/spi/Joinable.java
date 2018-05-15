/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.type.ForeignKeyDirection;

/**
 * Specialization for Navigable that can be used in creating TableGroupJoins
 *
 * @author Steve Ebersole
 */
public interface Joinable<T> extends Navigable<T> {
	// todo (6.0) : #createSqmJoin ?

	default ForeignKeyDirection getForeignKeyDirection() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	default boolean isCascadeDeleteEnabled() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
