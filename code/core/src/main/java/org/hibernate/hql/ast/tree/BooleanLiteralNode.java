package org.hibernate.hql.ast.tree;

import org.hibernate.type.Type;
import org.hibernate.type.BooleanType;
import org.hibernate.Hibernate;
import org.hibernate.QueryException;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * Represents a boolean literal within a query.
 *
 * @author Steve Ebersole
 */
public class BooleanLiteralNode extends LiteralNode implements ExpectedTypeAwareNode {
	private Type expectedType;

	public Type getDataType() {
		return expectedType == null ? Hibernate.BOOLEAN : expectedType;
	}

	public BooleanType getTypeInternal() {
		return ( BooleanType ) getDataType();
	}

	public Boolean getValue() {
		return getType() == TRUE ? Boolean.TRUE : Boolean.FALSE;
	}

	/**
	 * Expected-types really only pertinent here for boolean literals...
	 *
	 * @param expectedType
	 */
	public void setExpectedType(Type expectedType) {
		this.expectedType = expectedType;
	}

	public Type getExpectedType() {
		return expectedType;
	}

	public String getRenderText(SessionFactoryImplementor sessionFactory) {
		try {
			return getTypeInternal().objectToSQLString( getValue(), sessionFactory.getDialect() );
		}
		catch( Throwable t ) {
			throw new QueryException( "Unable to render boolean literal value", t );
		}
	}
}
