/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.internal;

import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReferenceExpression;

/**
 * Seperate from org.hibernate.sql.ast.tree.internal.BasicScalarSelectionImpl because
 * here we need to know the "source table" as well as any attribute converters tro apply
 *
 * @author Steve Ebersole
 */
public class BasicValuedNavigableSelection extends AbstractBasicValuedSelection implements NavigableSelection {
	public BasicValuedNavigableSelection(
			NavigableReferenceExpression selectedExpression,
			String resultVariable) {
		super( selectedExpression, resultVariable );

	}

	@Override
	public NavigableReferenceExpression getSelectedExpression() {
		return (NavigableReferenceExpression) super.getSelectedExpression();
	}
}
