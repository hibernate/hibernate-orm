/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.internal;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableContainerReferenceInfo;

/**
 * @author Steve Ebersole
 */
public class NavigableContainerReferenceInfoImpl
		extends NavigableReferenceInfoImpl
		implements NavigableContainerReferenceInfo {
	public NavigableContainerReferenceInfoImpl(
			NavigableContainerReferenceInfoImpl containerReferenceInfo,
			NavigableContainer navigable,
			NavigablePath path,
			String uniqueIdentifier,
			String identificationVariable,
			EntityTypeDescriptor intrinsicDowncastTarget) {
		super(
				containerReferenceInfo,
				navigable,
				path,
				uniqueIdentifier,
				identificationVariable,
				intrinsicDowncastTarget
		);
	}

	@Override
	public NavigableContainer getReferencedNavigable() {
		return (NavigableContainer) super.getReferencedNavigable();
	}
}
