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
public interface CollectionElementEmbedded<J> extends CollectionElement<J>, EmbeddedValuedNavigable<J> {
	@Override
	default boolean canContainSubGraphs() {
		return true;
	}

	@Override
	default ElementClassification getClassification() {
		return ElementClassification.EMBEDDABLE;
	}

	@Override
	default void visitNavigable(NavigableVisitationStrategy visitor) {
		// visit ourself
		visitor.visitCollectionElementEmbedded( this );
	}

	@Override
	default Table getPrimaryDmlTable() {
		return getCollectionDescriptor().getSeparateCollectionTable();
	}
}
