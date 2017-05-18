/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.metamodel.model.relational.spi.Column;

/**
 * @author Steve Ebersole
 */
public interface TenantDiscrimination extends Navigable {
	Column getColumn();

	boolean isShared();

	boolean isUseParameterBinding();

	// todo (6.0) : VirtualNavigable?
	//		see note on VirtualPersistentAttribute

	@Override
	default void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitTenantTenantDiscrimination( this );
	}
}
