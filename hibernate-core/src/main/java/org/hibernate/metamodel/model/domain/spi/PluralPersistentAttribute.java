/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

/**
 * @author Steve Ebersole
 */
public interface PluralPersistentAttribute<O,C,E>
		extends PersistentAttribute<O,C>, NavigableContainer<C>, javax.persistence.metamodel.PluralAttribute<O,C,E> {
	PersistentCollectionMetadata<O,C,E> getPersistentCollectionMetadata();

	@Override
	default void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitPluralAttribute( this );
	}
}
