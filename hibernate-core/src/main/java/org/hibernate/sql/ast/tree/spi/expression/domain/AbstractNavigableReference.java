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
public abstract class AbstractNavigableReference implements NavigableReference {
	private final NavigableContainerReference containerReference;
	private final Navigable navigable;
	private final NavigablePath navigablePath;

	private final ColumnReferenceQualifier columnReferenceQualifier;

	public AbstractNavigableReference(
			NavigableContainerReference containerReference,
			Navigable navigable,
			NavigablePath navigablePath,
			ColumnReferenceQualifier columnReferenceQualifier) {
		this.containerReference = containerReference;
		this.navigable = navigable;
		this.navigablePath = navigablePath;
		this.columnReferenceQualifier = columnReferenceQualifier;
	}

	@Override
	public Navigable getNavigable() {
		return navigable;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public NavigableContainerReference getNavigableContainerReference() {
		return containerReference;
	}

	@Override
	public ColumnReferenceQualifier getColumnReferenceQualifier() {
		return columnReferenceQualifier;
	}
}
