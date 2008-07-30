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

import org.hibernate.param.ParameterSpecification;
import org.hibernate.type.Type;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * Implementation of ParameterNode.
 *
 * @author Steve Ebersole
 */
public class ParameterNode extends HqlSqlWalkerNode implements DisplayableNode, ExpectedTypeAwareNode {
	private ParameterSpecification parameterSpecification;

	public ParameterSpecification getHqlParameterSpecification() {
		return parameterSpecification;
	}

	public void setHqlParameterSpecification(ParameterSpecification parameterSpecification) {
		this.parameterSpecification = parameterSpecification;
	}

	public String getDisplayText() {
		return "{" + ( parameterSpecification == null ? "???" : parameterSpecification.renderDisplayInfo() ) + "}";
	}

	public void setExpectedType(Type expectedType) {
		getHqlParameterSpecification().setExpectedType( expectedType );
		setDataType( expectedType );
	}

	public Type getExpectedType() {
		return getHqlParameterSpecification() == null ? null : getHqlParameterSpecification().getExpectedType();
	}

	public String getRenderText(SessionFactoryImplementor sessionFactory) {
		int count = 0;
		if ( getExpectedType() != null && ( count = getExpectedType().getColumnSpan( sessionFactory ) ) > 1 ) {
			StringBuffer buffer = new StringBuffer();
			buffer.append( "(?" );
			for ( int i = 1; i < count; i++ ) {
				buffer.append( ", ?" );
			}
			buffer.append( ")" );
			return buffer.toString();
		}
		else {
			return "?";
		}
	}
}
