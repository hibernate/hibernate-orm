/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import org.hibernate.metamodel.mapping.ordering.TranslationContext;

/**
 * Represents a column-reference used in an order-by fragment
 *
 * @apiNote This is Hibernate-specific feature.  For {@link javax.persistence.OrderBy} (JPA)
 * all path references are expected to be domain paths (attributes).
 *
 * @author Steve Ebersole
 */
public class ColumnReference implements SortExpression, SequencePart {
	private final String columnExpression;

	public ColumnReference(String columnExpression) {
		this.columnExpression = columnExpression;
	}

	public String getColumnExpression() {
		return columnExpression;
	}

	@Override
	public SequencePart resolvePathPart(
			String name,
			boolean isTerminal,
			TranslationContext translationContext) {
		throw new UnsupportedOperationException( "ColumnReference cannot be de-referenced" );
	}
}
