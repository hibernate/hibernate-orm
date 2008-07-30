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

import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;
import org.hibernate.type.LiteralType;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.StringHelper;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.QueryException;
import org.hibernate.hql.QueryTranslator;

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

	public void setText(String s) {
		// for some reason the antlr.CommonAST initialization routines force
		// this method to get called twice.  The first time with an empty string
		if ( StringHelper.isNotEmpty( s ) ) {
			constantExpression = s;
			constantValue = ReflectHelper.getConstantValue( s );
			heuristicType = TypeFactory.heuristicType( constantValue.getClass().getName() );
			super.setText( s );
		}
	}

	public void setExpectedType(Type expectedType) {
		this.expectedType = expectedType;
	}

	public Type getExpectedType() {
		return expectedType;
	}

	public void setSessionFactory(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	private String resolveToLiteralString(Type type) {
		try {
			LiteralType literalType = ( LiteralType ) type;
			Dialect dialect = factory.getDialect();
			return literalType.objectToSQLString( constantValue, dialect );
		}
		catch ( Throwable t ) {
			throw new QueryException( QueryTranslator.ERROR_CANNOT_FORMAT_LITERAL + constantExpression, t );
		}
	}

	public String getRenderText(SessionFactoryImplementor sessionFactory) {
		Type type = expectedType == null ? heuristicType : expectedType;
		return resolveToLiteralString( type );
	}
}
