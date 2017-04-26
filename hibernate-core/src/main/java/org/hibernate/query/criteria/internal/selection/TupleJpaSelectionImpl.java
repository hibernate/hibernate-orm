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
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.produce.spi.criteria.CriteriaVisitor;
import org.hibernate.query.sqm.produce.spi.criteria.JpaExpression;
import org.hibernate.query.sqm.tree.select.SqmAliasedExpressionContainer;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;

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
