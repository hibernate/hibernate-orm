/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.metamodel.model.domain.spi.BasicValuedNavigable;
import org.hibernate.query.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class BasicValuedNavigableReference extends AbstractNavigableReference {

	public BasicValuedNavigableReference(
			NavigableContainerReference containerReference,
			BasicValuedNavigable referencedNavigable,
			NavigablePath navigablePath) {
		super( containerReference, referencedNavigable, navigablePath, containerReference.getColumnReferenceQualifier() );
	}

	@Override
	public BasicValuedNavigable getNavigable() {
		return (BasicValuedNavigable) super.getNavigable();
	}
}
