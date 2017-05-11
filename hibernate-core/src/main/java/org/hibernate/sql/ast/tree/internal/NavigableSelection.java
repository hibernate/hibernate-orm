/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.internal;

import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReferenceExpression;
import org.hibernate.sql.ast.tree.spi.select.Selection;

/**
 * @author Steve Ebersole
 */
public interface NavigableSelection extends Selection {
	@Override
	NavigableReferenceExpression getSelectedExpression();
}
