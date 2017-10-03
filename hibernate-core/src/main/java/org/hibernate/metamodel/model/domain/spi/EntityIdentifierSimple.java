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
public interface EntityIdentifierSimple<O,J>
		extends EntityIdentifier<O,J>, SingularPersistentAttribute<O,J>, BasicValuedNavigable<J> {
	@Override
	default void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitSimpleIdentifier( this );
	}

	@Override
	default PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.BASIC;
	}

	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}
}
