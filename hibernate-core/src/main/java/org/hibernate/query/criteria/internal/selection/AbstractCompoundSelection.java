/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.internal.selection;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.criteria.Selection;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.internal.expression.AbstractTupleElement;
import org.hibernate.sqm.parser.criteria.tree.JpaExpression;
import org.hibernate.sqm.parser.criteria.tree.select.JpaCompoundSelection;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCompoundSelection<X>
		extends AbstractTupleElement<X>
		implements JpaCompoundSelection<X>, Serializable {
	private List<JpaExpression<?>> expressions;

	public AbstractCompoundSelection(
			HibernateCriteriaBuilder criteriaBuilder,
			Class<X> javaType,
			List<JpaExpression<?>> expressions) {
		super( criteriaBuilder, javaType );
		this.expressions = expressions;
	}

	public List<JpaExpression<?>> getExpressions() {
		return expressions == null ? Collections.emptyList() : expressions;
	}

	public JpaCompoundSelection<X> alias(String alias) {
		setAlias( alias );
		return this;
	}

	public boolean isCompoundSelection() {
		return true;
	}

	@Override
	public List<Selection<?>> getCompoundSelectionItems() {
		return expressions == null ? Collections.emptyList() : expressions.stream().collect( Collectors.toList() );
	}
}
