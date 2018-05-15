/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.metamodel.model.relational.spi.DerivedColumn;

/**
 * @author Steve Ebersole
 */
public interface RowIdDescriptor<J> extends VirtualNavigable<J>, BasicValuedNavigable<J> {
	String NAVIGABLE_NAME = "{rowId}";

	@Override
	DerivedColumn getBoundColumn();

	@Override
	default void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitRowIdDescriptor( this );
	}

	@Override
	default Class<J> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	default boolean matchesNavigableName(J navigableName) {
		return NAVIGABLE_NAME.equals( navigableName );
	}
}
