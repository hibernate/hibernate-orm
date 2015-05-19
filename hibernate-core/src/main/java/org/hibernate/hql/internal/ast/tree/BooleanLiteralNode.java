/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.LiteralType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Represents a boolean literal within a query.
 *
 * @author Steve Ebersole
 */
public class BooleanLiteralNode extends LiteralNode implements ExpectedTypeAwareNode {
	private Type expectedType;

	public Type getDataType() {
		return expectedType == null ? StandardBasicTypes.BOOLEAN : expectedType;
	}

	public Boolean getValue() {
		return getType() == TRUE ;
	}

	@Override
	public void setExpectedType(Type expectedType) {
		this.expectedType = expectedType;
	}

	@Override
	public Type getExpectedType() {
		return expectedType;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public String getRenderText(SessionFactoryImplementor sessionFactory) {
		try {
			return typeAsLiteralType().objectToSQLString( getValue(), sessionFactory.getDialect() );
		}
		catch( Exception t ) {
			throw new QueryException( "Unable to render boolean literal value", t );
		}
	}

	private LiteralType typeAsLiteralType() {
		return (LiteralType) getDataType();

	}
}
