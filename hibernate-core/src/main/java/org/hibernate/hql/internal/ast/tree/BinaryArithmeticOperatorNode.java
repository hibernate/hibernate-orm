/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.ast.util.ColumnHelper;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import antlr.SemanticException;

/**
 * Nodes which represent binary arithmetic operators.
 *
 * @author Gavin King
 */
public class BinaryArithmeticOperatorNode extends AbstractSelectExpression
		implements BinaryOperatorNode, DisplayableNode {

	@Override
	public void initialize() throws SemanticException {
		final Node lhs = getLeftHandOperand();
		if ( lhs == null ) {
			throw new SemanticException( "left-hand operand of a binary operator was null" );
		}

		final Node rhs = getRightHandOperand();
		if ( rhs == null ) {
			throw new SemanticException( "right-hand operand of a binary operator was null" );
		}

		final Type lhType = ( lhs instanceof SqlNode ) ? ( (SqlNode) lhs ).getDataType() : null;
		final Type rhType = ( rhs instanceof SqlNode ) ? ( (SqlNode) rhs ).getDataType() : null;

		if ( ExpectedTypeAwareNode.class.isAssignableFrom( lhs.getClass() ) && rhType != null ) {
			final Type expectedType;
			// we have something like : "? [op] rhs"
			if ( isDateTimeType( rhType ) ) {
				// more specifically : "? [op] datetime"
				//      1) if the operator is MINUS, the param needs to be of
				//          some datetime type
				//      2) if the operator is PLUS, the param needs to be of
				//          some numeric type
				expectedType = getType() == HqlSqlTokenTypes.PLUS ? StandardBasicTypes.DOUBLE : rhType;
			}
			else {
				expectedType = rhType;
			}
			( (ExpectedTypeAwareNode) lhs ).setExpectedType( expectedType );
		}
		else if ( ParameterNode.class.isAssignableFrom( rhs.getClass() ) && lhType != null ) {
			Type expectedType = null;
			// we have something like : "lhs [op] ?"
			if ( isDateTimeType( lhType ) ) {
				// more specifically : "datetime [op] ?"
				//      1) if the operator is MINUS, we really cannot determine
				//          the expected type as either another datetime or
				//          numeric would be valid
				//      2) if the operator is PLUS, the param needs to be of
				//          some numeric type
				if ( getType() == HqlSqlTokenTypes.PLUS ) {
					expectedType = StandardBasicTypes.DOUBLE;
				}
			}
			else {
				expectedType = lhType;
			}
			( (ExpectedTypeAwareNode) rhs ).setExpectedType( expectedType );
		}
	}

	/**
	 * Figure out the type of the binary expression by looking at
	 * the types of the operands. Sometimes we don't know both types,
	 * if, for example, one is a parameter.
	 */
	@Override
	public Type getDataType() {
		if ( super.getDataType() == null ) {
			super.setDataType( resolveDataType() );
		}
		return super.getDataType();
	}

	private Type resolveDataType() {
		// TODO : we may also want to check that the types here map to exactly one column/JDBC-type
		//      can't think of a situation where arithmetic expression between multi-column mappings
		//      makes any sense.
		final Node lhs = getLeftHandOperand();
		final Node rhs = getRightHandOperand();

		final Type lhType = ( lhs instanceof SqlNode ) ? ( (SqlNode) lhs ).getDataType() : null;
		final Type rhType = ( rhs instanceof SqlNode ) ? ( (SqlNode) rhs ).getDataType() : null;

		if ( isDateTimeType( lhType ) || isDateTimeType( rhType ) ) {
			return resolveDateTimeArithmeticResultType( lhType, rhType );
		}
		else {
			if ( lhType == null ) {
				if ( rhType == null ) {
					// we do not know either type
					// BLIND GUESS!
					return StandardBasicTypes.DOUBLE;
				}
				else {
					// we know only the rhs-hand type, so use that
					return rhType;
				}
			}
			else {
				if ( rhType == null ) {
					// we know only the lhs-hand type, so use that
					return lhType;
				}
				else {
					if ( lhType == StandardBasicTypes.DOUBLE || rhType == StandardBasicTypes.DOUBLE ) {
						return StandardBasicTypes.DOUBLE;
					}
					if ( lhType == StandardBasicTypes.FLOAT || rhType == StandardBasicTypes.FLOAT ) {
						return StandardBasicTypes.FLOAT;
					}
					if ( lhType == StandardBasicTypes.BIG_DECIMAL || rhType == StandardBasicTypes.BIG_DECIMAL ) {
						return StandardBasicTypes.BIG_DECIMAL;
					}
					if ( lhType == StandardBasicTypes.BIG_INTEGER || rhType == StandardBasicTypes.BIG_INTEGER ) {
						return StandardBasicTypes.BIG_INTEGER;
					}
					if ( lhType == StandardBasicTypes.LONG || rhType == StandardBasicTypes.LONG ) {
						return StandardBasicTypes.LONG;
					}
					if ( lhType == StandardBasicTypes.INTEGER || rhType == StandardBasicTypes.INTEGER ) {
						return StandardBasicTypes.INTEGER;
					}
					return lhType;
				}
			}
		}
	}

	private boolean isDateTimeType(Type type) {
		return type != null
				&& ( java.util.Date.class.isAssignableFrom( type.getReturnedClass() )
				|| java.util.Calendar.class.isAssignableFrom( type.getReturnedClass() ) );
	}

	private Type resolveDateTimeArithmeticResultType(Type lhType, Type rhType) {
		// here, we work under the following assumptions:
		//      ------------ valid cases --------------------------------------
		//      1) datetime + {something other than datetime} : always results
		//              in a datetime ( db will catch invalid conversions )
		//      2) datetime - datetime : always results in a DOUBLE
		//      3) datetime - {something other than datetime} : always results
		//              in a datetime ( db will catch invalid conversions )
		//      ------------ invalid cases ------------------------------------
		//      4) datetime + datetime
		//      5) {something other than datetime} - datetime
		//      6) datetime * {any type}
		//      7) datetime / {any type}
		//      8) {any type} / datetime
		// doing so allows us to properly handle parameters as either the left
		// or right side here in the majority of cases
		boolean lhsIsDateTime = isDateTimeType( lhType );
		boolean rhsIsDateTime = isDateTimeType( rhType );

		// handle the (assumed) valid cases:
		// #1 - the only valid datetime addition synatx is one or the other is a datetime (but not both)
		if ( getType() == HqlSqlTokenTypes.PLUS ) {
			// one or the other needs to be a datetime for us to get into this method in the first place...
			return lhsIsDateTime ? lhType : rhType;
		}
		else if ( getType() == HqlSqlTokenTypes.MINUS ) {
			// #3 - note that this is also true of "datetime - :param"...
			if ( lhsIsDateTime && !rhsIsDateTime ) {
				return lhType;
			}
			// #2
			if ( lhsIsDateTime && rhsIsDateTime ) {
				return StandardBasicTypes.DOUBLE;
			}
		}
		return null;
	}

	@Override
	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateSingleScalarColumn( this, i );
	}

	/**
	 * Retrieves the left-hand operand of the operator.
	 *
	 * @return The left-hand operand
	 */
	@Override
	public Node getLeftHandOperand() {
		return (Node) getFirstChild();
	}

	/**
	 * Retrieves the right-hand operand of the operator.
	 *
	 * @return The right-hand operand
	 */
	@Override
	public Node getRightHandOperand() {
		return (Node) getFirstChild().getNextSibling();
	}

	@Override
	public String getDisplayText() {
		return "{dataType=" + getDataType() + "}";
	}
}
