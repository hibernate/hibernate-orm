/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.insert;

import org.hibernate.query.criteria.JpaValues;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Gavin King
 */
public class SqmValues implements JpaValues, Serializable {
	private final List<SqmExpression<?>> expressions;

	public SqmValues(List<SqmExpression<?>> expressions) {
		this.expressions = expressions;
	}

	private SqmValues(SqmValues original, SqmCopyContext context) {
		this.expressions = new ArrayList<>( original.expressions.size() );
		for ( SqmExpression<?> expression : original.expressions ) {
			this.expressions.add( expression.copy( context ) );
		}
	}

	public SqmValues copy(SqmCopyContext context) {
		return new SqmValues( this, context );
	}

	@Override
	public List<SqmExpression<?>> getExpressions() {
		return Collections.unmodifiableList( expressions );
	}
}
