/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import java.util.Locale;

import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.LiteralType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;

/**
 * A node representing a static Java constant.
 *
 * @author Steve Ebersole
 */
public class JavaConstantNode extends Node implements ExpectedTypeAwareNode, SessionFactoryAwareNode {
	private SessionFactoryImplementor factory;

	private String constantExpression;
	private Object constantValue;
	private Type heuristicType;

	private Type expectedType;

	@Override
	public void setText(String s) {
		// for some reason the antlr.CommonAST initialization routines force
		// this method to get called twice.  The first time with an empty string
		if ( StringHelper.isNotEmpty( s ) ) {
			constantExpression = s;
			constantValue = ReflectHelper.getConstantValue( s, factory );
			heuristicType = factory.getTypeResolver().heuristicType( constantValue.getClass().getName() );
			super.setText( s );
		}
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
	public void setSessionFactory(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	@Override
	@SuppressWarnings("unchecked")
	public String getRenderText(SessionFactoryImplementor sessionFactory) {
		final Type type = expectedType == null
				? heuristicType
				: Number.class.isAssignableFrom( heuristicType.getReturnedClass() )
				? heuristicType
				: expectedType;
		try {
			if ( LiteralType.class.isInstance( type ) ) {
				final LiteralType literalType = (LiteralType) type;
				final Dialect dialect = factory.getDialect();
				return literalType.objectToSQLString( constantValue, dialect );
			}
			else if ( AttributeConverterTypeAdapter.class.isInstance( type ) ) {
				final AttributeConverterTypeAdapter converterType = (AttributeConverterTypeAdapter) type;
				if ( !converterType.getModelType().isInstance( constantValue ) ) {
					throw new QueryException(
							String.format(
									Locale.ENGLISH,
									"Recognized query constant expression [%s] was not resolved to type [%s] expected by defined AttributeConverter [%s]",
									constantExpression,
									constantValue.getClass().getName(),
									converterType.getModelType().getName()
							)
					);
				}
				final Object value = converterType.getAttributeConverter().convertToDatabaseColumn( constantValue );
				if ( String.class.equals( converterType.getJdbcType() ) ) {
					return "'" + value + "'";
				}
				else {
					return value.toString();
				}
			}
			else {
				throw new QueryException(
						String.format(
								Locale.ENGLISH,
								"Unrecognized Hibernate Type for handling query constant (%s); expecting LiteralType implementation or AttributeConverter",
								constantExpression
						)
				);
			}
		}
		catch (QueryException e) {
			throw e;
		}
		catch (Exception t) {
			throw new QueryException( QueryTranslator.ERROR_CANNOT_FORMAT_LITERAL + constantExpression, t );
		}
	}
}
