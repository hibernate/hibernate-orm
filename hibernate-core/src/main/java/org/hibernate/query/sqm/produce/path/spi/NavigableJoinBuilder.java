/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.path.spi;

import org.hibernate.metamodel.model.domain.spi.BasicValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;

/**
 * @author Steve Ebersole
 */
public interface NavigableJoinBuilder {
	void buildNavigableJoinIfNecessary(SqmNavigableReference navigableReference, boolean isTerminal);

	default boolean isJoinable(SqmNavigableReference navigableReference) {
		final Navigable navigable = navigableReference.getReferencedNavigable();
		if ( SingularPersistentAttribute.class.isInstance( navigable ) ) {
			return !BasicValuedNavigable.class.isInstance( navigable );
		}
		else if ( PluralPersistentAttribute.class.isInstance( navigable ) ) {
			// plural attributes can always be joined.
			return true;
		}
		else {
			return false;
		}
	}

}
