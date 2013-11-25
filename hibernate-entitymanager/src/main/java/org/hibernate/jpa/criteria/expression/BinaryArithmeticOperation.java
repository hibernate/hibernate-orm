/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.criteria.expression;

import java.io.Serializable;
import javax.persistence.criteria.Expression;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.ParameterRegistry;
import org.hibernate.jpa.criteria.Renderable;
import org.hibernate.jpa.criteria.compile.RenderingContext;
import org.hibernate.jpa.criteria.predicate.ImplicitNumericExpressionTypeDeterminer;

/**
 * Models standard arithmetc operations with two operands.
 *
 * @author Steve Ebersole
 */
public class BinaryArithmeticOperation<N extends Number>
		extends ExpressionImpl<N>
		implements BinaryOperatorExpression<N>, Serializable {

	public static enum Operation {
		ADD {
			@Override
			String apply(String lhs, String rhs) {
				return applyPrimitive( lhs, '+', rhs );
			}
		},
		SUBTRACT {
			@Override
			String apply(String lhs, String rhs) {
				return applyPrimitive( lhs, '-', rhs );
			}
		},
		MULTIPLY {
			@Override
			String apply(String lhs, String rhs) {
				return applyPrimitive( lhs, '*', rhs );
			}
		},
		DIVIDE {
			@Override
			String apply(String lhs, String rhs) {
				return applyPrimitive( lhs, '/', rhs );
			}
		},
		QUOT {
			@Override
			String apply(String lhs, String rhs) {
				return applyPrimitive( lhs, '/', rhs );
			}
		},
		MOD {
			@Override
			String apply(String lhs, String rhs) {
//				return lhs + " % " + rhs;
				return "mod(" + lhs + "," + rhs + ")";
			}
		};

		abstract String apply(String lhs, String rhs);

		private static final char LEFT_PAREN = '(';
		private static final char RIGHT_PAREN = ')';

		private static String applyPrimitive(String lhs, char operator, String rhs) {
			return String.valueOf( LEFT_PAREN ) + lhs + operator + rhs + RIGHT_PAREN;
		}
	}

	private final Operation operator;
	private final Expression<? extends N> rhs;
	private final Expression<? extends N> lhs;

	public static Class<? extends Number> determineResultType(
			Class<? extends Number> argument1Type,
			Class<? extends Number> argument2Type
	) {
		return determineResultType( argument1Type, argument2Type, false );
	}

	public static Class<? extends Number> determineResultType(
			Class<? extends Number> argument1Type,
			Class<? extends Number> argument2Type,
			boolean isQuotientOperation) {
		if ( isQuotientOperation ) {
			return Number.class;
		}
		return ImplicitNumericExpressionTypeDeterminer.determineResultType( argument1Type, argument2Type );
	}

	/**
	 * Helper for determining the appropriate operation return type based on one of the operands as an expression.
	 *
	 * @param defaultType The default return type to use if we cannot determine the java type of 'expression' operand.
	 * @param expression The operand.
	 *
	 * @return The appropriate return type.
	 */
	public static Class<? extends Number> determineReturnType(
			Class<? extends Number> defaultType,
			Expression<? extends Number> expression) {
		return expression == null || expression.getJavaType() == null 
				? defaultType
				: expression.getJavaType();
	}

	/**
	 * Helper for determining the appropriate operation return type based on one of the operands as a literal.
	 *
	 * @param defaultType The default return type to use if we cannot determine the java type of 'numberLiteral' operand.
	 * @param numberLiteral The operand.
	 *
	 * @return The appropriate return type.
	 */
	public static Class<? extends Number> determineReturnType(
			Class<? extends Number> defaultType,
			Number numberLiteral) {
		return numberLiteral == null ? defaultType : numberLiteral.getClass();
	}

	/**
	 * Creates an arithmethic operation based on 2 expressions.
	 *
	 * @param criteriaBuilder The builder for query components.
	 * @param resultType The operation result type
	 * @param operator The operator (type of operation).
	 * @param lhs The left-hand operand.
	 * @param rhs The right-hand operand
	 */
	public BinaryArithmeticOperation(
			CriteriaBuilderImpl criteriaBuilder,
			Class<N> resultType,
			Operation operator,
			Expression<? extends N> lhs,
			Expression<? extends N> rhs) {
		super( criteriaBuilder, resultType );
		this.operator = operator;
		this.lhs = lhs;
		this.rhs = rhs;
	}

	/**
	 * Creates an arithmethic operation based on an expression and a literal.
	 *
	 * @param criteriaBuilder The builder for query components.
	 * @param javaType The operation result type
	 * @param operator The operator (type of operation).
	 * @param lhs The left-hand operand
	 * @param rhs The right-hand operand (the literal)
	 */
	public BinaryArithmeticOperation(
			CriteriaBuilderImpl criteriaBuilder,
			Class<N> javaType,
			Operation operator,
			Expression<? extends N> lhs,
			N rhs) {
		super( criteriaBuilder, javaType );
		this.operator = operator;
		this.lhs = lhs;
		this.rhs = new LiteralExpression<N>( criteriaBuilder, rhs );
	}

	/**
	 * Creates an arithmetic operation based on an expression and a literal.
	 *
	 * @param criteriaBuilder The builder for query components.
	 * @param javaType The operation result type
	 * @param operator The operator (type of operation).
	 * @param lhs The left-hand operand (the literal)
	 * @param rhs The right-hand operand
	 */
	public BinaryArithmeticOperation(
			CriteriaBuilderImpl criteriaBuilder,
			Class<N> javaType,
			Operation operator,
			N lhs,
			Expression<? extends N> rhs) {
		super( criteriaBuilder, javaType );
		this.operator = operator;
		this.lhs = new LiteralExpression<N>( criteriaBuilder, lhs );
		this.rhs = rhs;
	}
	public Operation getOperator() {
		return operator;
	}

	@Override
	public Expression<? extends N> getRightHandOperand() {
		return rhs;
	}

	@Override
	public Expression<? extends N> getLeftHandOperand() {
		return lhs;
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getRightHandOperand(), registry );
		Helper.possibleParameter( getLeftHandOperand(), registry );
	}

	@Override
	public String render(RenderingContext renderingContext) {
		return getOperator().apply(
				( (Renderable) getLeftHandOperand() ).render( renderingContext ),
				( (Renderable) getRightHandOperand() ).render( renderingContext )
		);
	}

	@Override
	public String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
