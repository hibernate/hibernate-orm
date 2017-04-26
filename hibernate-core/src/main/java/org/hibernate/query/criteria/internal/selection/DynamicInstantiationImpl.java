/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.criteria.internal.selection;

import java.util.List;
import java.util.Map;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.sqm.produce.spi.criteria.CriteriaVisitor;
import org.hibernate.query.sqm.produce.spi.criteria.JpaExpression;
import org.hibernate.query.sqm.tree.select.SqmAliasedExpressionContainer;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiationImpl<T> extends AbstractCompoundSelection<T> {
	public DynamicInstantiationImpl(
			HibernateCriteriaBuilder criteriaBuilder,
			Class<T> target,
			List<JpaExpression<?>> arguments) {
		super( criteriaBuilder, target, arguments );
	}

	@Override
	public void visitSelections(CriteriaVisitor visitor, SqmAliasedExpressionContainer container) {
		final SqmDynamicInstantiation dynamicInstantiation;

		if ( List.class.equals( getJavaType() ) ) {
			dynamicInstantiation = SqmDynamicInstantiation.forListInstantiation();
		}
		else if ( Map.class.equals( getJavaType() ) ) {
			dynamicInstantiation = SqmDynamicInstantiation.forMapInstantiation();
		}
		else {
			dynamicInstantiation = SqmDynamicInstantiation.forClassInstantiation( getJavaType() );
		}

		for ( JpaExpression<?> argument : getExpressions() ) {
			argument.visitSelections( visitor, dynamicInstantiation );
		}

		container.add( dynamicInstantiation, getAlias() );
	}
}
