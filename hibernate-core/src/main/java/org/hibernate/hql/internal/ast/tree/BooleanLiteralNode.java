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
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;

/**
 * Represents a boolean literal within a query.
 *
 * @author Steve Ebersole
 */
public class BooleanLiteralNode extends LiteralNode implements ExpectedTypeAwareNode {
	private Type expectedType;

	public Type getDataType() {
		return getExpectedType() == null ? StandardBasicTypes.BOOLEAN : getExpectedType();
	}

	public Boolean getValue() {
		return getType() == TRUE ;
	}

	@Override
	public void setText(String s) {
		super.setText( s );
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
		final boolean literalValue = getValue();

		if ( expectedType instanceof AttributeConverterTypeAdapter ) {
			return determineConvertedValue( (AttributeConverterTypeAdapter) expectedType, literalValue );
		}
		else if ( expectedType instanceof LiteralType ) {
			try {
				return ( (LiteralType) expectedType ).objectToSQLString( getValue(), sessionFactory.getDialect() );
			}
			catch( Exception t ) {
				throw new QueryException( "Unable to render boolean literal value using expected LiteralType", t );
			}
		}

		return sessionFactory.getDialect().toBooleanValueString( literalValue );
	}
}
