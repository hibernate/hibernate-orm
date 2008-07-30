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
