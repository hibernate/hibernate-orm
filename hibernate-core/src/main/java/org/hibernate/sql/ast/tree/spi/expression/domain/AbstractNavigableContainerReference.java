/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNavigableContainerReference extends AbstractNavigableReference implements NavigableContainerReference {
	private final LockMode lockMode;

	private Map<String,NavigableReference> navigableReferenceMap;

	public AbstractNavigableContainerReference(
			NavigableContainerReference ourContainerReference,
			NavigableContainer navigable,
			NavigablePath navigablePath,
			ColumnReferenceQualifier columnReferenceQualifier,
			LockMode lockMode) {
		super( ourContainerReference, navigable, navigablePath, columnReferenceQualifier );
		this.lockMode = lockMode;
	}

	@Override
	public NavigableContainer getNavigable() {
		return (NavigableContainer) super.getNavigable();
	}

	public LockMode getLockMode() {
		return lockMode;
	}

	@Override
	public NavigableReference findNavigableReference(String navigableName) {
		// todo (6.0) : make sure we account for cases where we cannot share named navigable references
		//		e.g., the same relative attribute joined twice to different identification variables
		return navigableReferenceMap == null ? null : navigableReferenceMap.get( navigableName );
	}

	@Override
	public void addNavigableReference(NavigableReference reference) {
		// todo (6.0) : conversely, make sure we do not cache non-shareable navigable references
		if ( navigableReferenceMap == null ) {
			navigableReferenceMap = new HashMap<>();
			navigableReferenceMap.put(
					reference.getNavigable().getNavigableName(),
					reference
			);
		}
		else {
			final NavigableReference previous = navigableReferenceMap.put(
					reference.getNavigable().getNavigableName(),
					reference
			);

			if ( previous != null && previous != reference ) {
				throw new IllegalStateException(
						String.format(
								Locale.ROOT,
								"Attempt to register multiple NavigableRefereces [%s, %s] under single name [%s] in NavigableReferenceContainer [%s]",
								previous,
								reference,
								reference.getNavigable().getNavigableName(),
								this
						)
				);
			}
		}
	}
}
