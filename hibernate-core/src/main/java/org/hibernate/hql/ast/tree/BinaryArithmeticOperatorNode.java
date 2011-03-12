/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.hql.ast.tree;

import org.hibernate.Hibernate;
import org.hibernate.hql.ast.util.ColumnHelper;
import org.hibernate.hql.antlr.HqlSqlTokenTypes;
import org.hibernate.type.Type;

import antlr.SemanticException;

/**
 * Nodes which represent binary arithmetic operators.
 *
 * @author Gavin King
 */
public class BinaryArithmeticOperatorNode extends AbstractSelectExpression implements BinaryOperatorNode, DisplayableNode {

	public void initialize() throws SemanticException {
		Node lhs = getLeftHandOperand();
		Node rhs = getRightHandOperand();
		if ( lhs == null ) {
			throw new SemanticException( "left-hand operand of a binary operator was null" );
		}
		if ( rhs == null ) {
			throw new SemanticException( "right-hand operand of a binary operator was null" );
		}

		Type lhType = ( lhs instanceof SqlNode ) ? ( ( SqlNode ) lhs ).getDataType() : null;
		Type rhType = ( rhs instanceof SqlNode ) ? ( ( SqlNode ) rhs ).getDataType() : null;

		if ( ExpectedTypeAwareNode.class.isAssignableFrom( lhs.getClass() ) && rhType != null ) {
			Type expectedType = null;
			// we have something like : "? [op] rhs"
			if ( isDateTimeType( rhType ) ) {
				// more specifically : "? [op] datetime"
				//      1) if the operator is MINUS, the param needs to be of
				//          some datetime type
				//      2) if the operator is PLUS, the param needs to be of
				//          some numeric type
				expectedType = getType() == HqlSqlTokenTypes.PLUS ? Hibernate.DOUBLE : rhType;
			}
			else {
				expectedType = rhType;
			}
			( ( ExpectedTypeAwareNode ) lhs ).setExpectedType( expectedType );
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
					expectedType = Hibernate.DOUBLE;
				}
			}
			else {
				expectedType = lhType;
			}
			( ( ExpectedTypeAwareNode ) rhs ).setExpectedType( expectedType );
		}
	}

	/**
	 * Figure out the type of the binary expression by looking at
	 * the types of the operands. Sometimes we don't know both types,
	 * if, for example, one is a parameter.
	 */
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
		Node lhs = getLeftHandOperand();
		Node rhs = getRightHandOperand();
		Type lhType = ( lhs instanceof SqlNode ) ? ( ( SqlNode ) lhs ).getDataType() : null;
		Type rhType = ( rhs instanceof SqlNode ) ? ( ( SqlNode ) rhs ).getDataType() : null;
		if ( isDateTimeType( lhType ) || isDateTimeType( rhType ) ) {
			return resolveDateTimeArithmeticResultType( lhType, rhType );
		}
		else {
			if ( lhType == null ) {
				if ( rhType == null ) {
					// we do not know either type
					return Hibernate.DOUBLE; //BLIND GUESS!
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
					if ( lhType==Hibernate.DOUBLE || rhType==Hibernate.DOUBLE ) return Hibernate.DOUBLE;
					if ( lhType==Hibernate.FLOAT || rhType==Hibernate.FLOAT ) return Hibernate.FLOAT;
					if ( lhType==Hibernate.BIG_DECIMAL || rhType==Hibernate.BIG_DECIMAL ) return Hibernate.BIG_DECIMAL;
					if ( lhType==Hibernate.BIG_INTEGER || rhType==Hibernate.BIG_INTEGER ) return Hibernate.BIG_INTEGER;
					if ( lhType==Hibernate.LONG || rhType==Hibernate.LONG ) return Hibernate.LONG;
					if ( lhType==Hibernate.INTEGER || rhType==Hibernate.INTEGER ) return Hibernate.INTEGER;
					return lhType;
				}
			}
		}
	}

	private boolean isDateTimeType(Type type) {
		if ( type == null ) {
			return false;
		}
		return java.util.Date.class.isAssignableFrom( type.getReturnedClass() ) ||
	           java.util.Calendar.class.isAssignableFrom( type.getReturnedClass() );
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
				return Hibernate.DOUBLE;
			}
		}
		return null;
	}

	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateSingleScalarColumn( this, i );
	}

	/**
	 * Retrieves the left-hand operand of the operator.
	 *
	 * @return The left-hand operand
	 */
	public Node getLeftHandOperand() {
		return ( Node ) getFirstChild();
	}

	/**
	 * Retrieves the right-hand operand of the operator.
	 *
	 * @return The right-hand operand
	 */
	public Node getRightHandOperand() {
		return ( Node ) getFirstChild().getNextSibling();
	}

	public String getDisplayText() {
		return "{dataType=" + getDataType() + "}";
	}
}
