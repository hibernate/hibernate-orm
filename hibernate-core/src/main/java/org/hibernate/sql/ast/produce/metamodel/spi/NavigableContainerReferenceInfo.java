/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import org.hibernate.metamodel.model.domain.spi.NavigableContainer;

/**
 * A container for NavigableReferenceInfo objects
 *
 * @author Steve Ebersole
 */
public interface NavigableContainerReferenceInfo extends NavigableReferenceInfo {
	@Override
	NavigableContainer getReferencedNavigable();
}
