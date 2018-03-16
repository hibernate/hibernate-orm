/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.path;

import java.io.Serializable;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.Attribute;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.CriteriaSubqueryImpl;
import org.hibernate.query.criteria.internal.FromImplementor;
import org.hibernate.query.criteria.internal.JoinImplementor;
import org.hibernate.query.criteria.internal.PathSource;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.predicate.PredicateImplementor;

/**
 * Convenience base class for various {@link javax.persistence.criteria.Join} implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJoinImpl<Z, X>
		extends AbstractFromImpl<Z, X>
		implements JoinImplementor<Z,X>, Serializable {

	private final Attribute<? super Z, ?> joinAttribute;
	private final JoinType joinType;

	private Predicate suppliedJoinCondition;

	public AbstractJoinImpl(
			CriteriaBuilderImpl criteriaBuilder,
			PathSource<Z> pathSource,
			Attribute<? super Z, X> joinAttribute,
			JoinType joinType) {
		this( criteriaBuilder, joinAttribute.getJavaType(), pathSource, joinAttribute, joinType );
	}

	public AbstractJoinImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Class<X> javaType,
			PathSource<Z> pathSource,
			Attribute<? super Z, ?> joinAttribute,
			JoinType joinType) {
		super( criteriaBuilder, javaType, pathSource );
		this.joinAttribute = joinAttribute;
		this.joinType = joinType;
	}

	@Override
	public Attribute<? super Z, ?> getAttribute() {
		return joinAttribute;
	}

	@Override
	public JoinType getJoinType() {
		return joinType;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public From<?, Z> getParent() {
		// this cast should be ok by virtue of our constructors...
		return (From<?, Z>) getPathSource();
	}

	@Override
	public String renderTableExpression(RenderingContext renderingContext) {
		prepareAlias( renderingContext );
		( (FromImplementor) getParent() ).prepareAlias( renderingContext );
		StringBuilder tableExpression = new StringBuilder();
		tableExpression.append( getParent().getAlias() )
				.append( '.' )
				.append( getAttribute().getName() )
				.append( " as " )
				.append( getAlias() );
		if ( suppliedJoinCondition != null ) {
			tableExpression.append( " with " )
					.append( ( (PredicateImplementor) suppliedJoinCondition ).render( renderingContext ) );
		}
		return tableExpression.toString();
	}

	@Override
	public JoinImplementor<Z, X> correlateTo(CriteriaSubqueryImpl subquery) {
		return (JoinImplementor<Z, X>) super.correlateTo( subquery );
	}

	@Override
	public JoinImplementor<Z, X> on(Predicate... restrictions) {
		// no matter what, a call to this method replaces any previously set values...
		this.suppliedJoinCondition = null;

		if ( restrictions != null && restrictions.length > 0 ) {
			this.suppliedJoinCondition = criteriaBuilder().and( restrictions );
		}

		return this;
	}

	@Override
	public JoinImplementor<Z, X> on(Expression<Boolean> restriction) {
		this.suppliedJoinCondition = criteriaBuilder().wrap( restriction );
		return this;
	}

	@Override
	public Predicate getOn() {
		return suppliedJoinCondition;
	}
}
