/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.metamodel.model.relational.spi.Table;

/**
 * @author Steve Ebersole
 */
public interface BasicCollectionElement<J>
		extends CollectionElement<J>, BasicValuedNavigable<J> {
	@Override
	default boolean canContainSubGraphs() {
		return false;
	}

	@Override
	default void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitCollectionElementBasic( this );
	}

	@Override
	default Table getPrimaryDmlTable() {
		return getCollectionDescriptor().getSeparateCollectionTable();
	}
}
