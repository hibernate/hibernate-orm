/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.criteria.internal.selection;

import java.util.List;
import javax.persistence.Tuple;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.sqm.parser.ParsingException;
import org.hibernate.sqm.parser.criteria.tree.CriteriaVisitor;
import org.hibernate.sqm.parser.criteria.tree.JpaExpression;
import org.hibernate.sqm.query.select.SqmAliasedExpressionContainer;
import org.hibernate.sqm.query.select.SqmSelectClause;

/**
 * @author Steve Ebersole
 */
public class TupleJpaSelectionImpl extends AbstractCompoundSelection<Tuple> {
	public TupleJpaSelectionImpl(
			HibernateCriteriaBuilder criteriaBuilder,
			Class<Tuple> javaType, List<JpaExpression<?>> expressions) {
		super( criteriaBuilder, javaType, expressions );
	}

	@Override
	public void visitSelections(CriteriaVisitor visitor, SqmAliasedExpressionContainer container) {
		if ( !SqmSelectClause.class.isInstance( container ) ) {
			throw new ParsingException( "Tuple selections are only valid as roots" );
		}

		for ( JpaExpression<?> jpaExpression : getExpressions() ) {
			container.add(
					jpaExpression.visitExpression( visitor ),
					jpaExpression.getAlias()
			);
		}
	}
}
