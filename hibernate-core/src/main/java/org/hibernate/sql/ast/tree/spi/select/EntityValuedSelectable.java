/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.select;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;

/**
 * @author Steve Ebersole
 */
public class EntityValuedSelectable implements Selectable {
	private final NavigableReference navigableReference;
	private final NavigablePath navigablePath;
	private final ColumnReferenceSource columnBindingSource;
	private final EntityDescriptor<?> entityDescriptor;
	private final boolean isShallow;

	public EntityValuedSelectable(
			NavigableReference navigableReference,
			NavigablePath navigablePath,
			ColumnReferenceSource columnBindingSource,
			boolean isShallow) {
		this.navigableReference = navigableReference;
		this.navigablePath = navigablePath;
		this.columnBindingSource = columnBindingSource;
		this.isShallow = isShallow;

		assert navigableReference.getNavigable() instanceof EntityValuedNavigable;

		final EntityValuedNavigable navigable = (EntityValuedNavigable) navigableReference.getNavigable();
		this.entityDescriptor = navigable.getEntityDescriptor();
	}

	@Override
	public Selection createSelection(Expression selectedExpression, String resultVariable) {
		assert selectedExpression instanceof NavigableReference;
		final NavigableReference navigableReference = (NavigableReference) selectedExpression;

		assert navigableReference.getNavigable() instanceof EntityValuedNavigable;

		final EntityValuedNavigable navigable = (EntityValuedNavigable) navigableReference.getNavigable();
		return navigable.getEntityDescriptor().createSelection( selectedExpression, resultVariable );
	}

}
