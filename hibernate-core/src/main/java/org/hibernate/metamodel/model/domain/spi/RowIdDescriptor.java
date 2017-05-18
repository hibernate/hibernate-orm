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
public interface RowIdDescriptor<O,J> extends VirtualPersistentAttribute<O,J>, SingularPersistentAttribute<O,J> {
	String NAVIGABLE_NAME = "{rowId}";

	@Override
	default void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitRowIdDescriptor( this );
	}
}
