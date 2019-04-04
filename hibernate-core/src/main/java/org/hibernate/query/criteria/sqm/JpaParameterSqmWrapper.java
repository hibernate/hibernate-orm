/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.sqm;

import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.criteria.spi.ParameterExpression;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Wraps a JPA criteria parameter as a SQM parameter.  The exact parameter
 * instance is used to set values, so we need to use the exact instance
 * in the Query we ultimately build
 *
 * @see ParameterMetadata
 *
 * @author Steve Ebersole
 */
public class JpaParameterSqmWrapper implements SqmParameter {
	private final ParameterExpression jpaParameterExpression;
	private final boolean allowMultiValuedBinding;

	private AllowableParameterType<?> parameterType;

	public JpaParameterSqmWrapper(ParameterExpression jpaParameterExpression, boolean allowMultiValuedBinding) {
		this.jpaParameterExpression = jpaParameterExpression;
		this.allowMultiValuedBinding = allowMultiValuedBinding;

		this.parameterType = jpaParameterExpression.getExplicitType();
	}

	public ParameterExpression<?> getJpaParameterExpression() {
		return jpaParameterExpression;
	}

	@Override
	public String getName() {
		return jpaParameterExpression.getName();
	}

	@Override
	public Integer getPosition() {
		// for criteria anyway, these cannot be positional
		return null;
	}

	@Override
	public boolean allowMultiValuedBinding() {
		return allowMultiValuedBinding;
	}

	@Override
	public AllowableParameterType getAnticipatedType() {
		return getExpressableType();
	}

	@Override
	public AllowableParameterType<?> getExpressableType() {
		return parameterType;
	}

	@Override
	public SqmParameter copy() {
		return new JpaParameterSqmWrapper( getJpaParameterExpression(), allowMultiValuedBinding() );
	}

	@Override
	public void applyInferableType(ExpressableType<?> type) {
		this.parameterType = (AllowableParameterType<?>) QueryHelper.highestPrecedenceType( parameterType, type );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getExpressableType().getJavaTypeDescriptor();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitJpaParameterWrapper( this );
	}
}
