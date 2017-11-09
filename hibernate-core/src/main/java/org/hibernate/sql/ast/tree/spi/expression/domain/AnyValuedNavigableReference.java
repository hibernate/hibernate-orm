/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;

/**
 * @author Steve Ebersole
 */
public class AnyValuedNavigableReference extends AbstractNavigableReference {
	public AnyValuedNavigableReference(
			NavigableContainerReference containerReference,
			Navigable navigable,
			NavigablePath navigablePath,
			ColumnReferenceQualifier columnReferenceQualifier) {
		super( containerReference, navigable, navigablePath, columnReferenceQualifier );
	}
}
