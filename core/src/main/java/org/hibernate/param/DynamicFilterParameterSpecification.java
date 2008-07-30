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
package org.hibernate.param;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;

/**
 * A specialized ParameterSpecification impl for dealing with a dynamic filter
 * parameters.
 * <p/>
 * Note: this class is currently not used.  The ideal way to deal with dynamic filter
 * parameters for HQL would be to track them just as we do with other parameters
 * in the translator.  However, the problem with that is that we currently do not
 * know the filters which actually apply to the query; we know the active/enabled ones,
 * but not the ones that actually "make it into" the resulting query.
 *
 * @author Steve Ebersole
 */
public class DynamicFilterParameterSpecification implements ParameterSpecification {
	private final String filterName;
	private final String parameterName;
	private final Type definedParameterType;
	private final int queryParameterPosition;

	public DynamicFilterParameterSpecification(
			String filterName,
			String parameterName,
			Type definedParameterType,
			int queryParameterPosition) {
		this.filterName = filterName;
		this.parameterName = parameterName;
		this.definedParameterType = definedParameterType;
		this.queryParameterPosition = queryParameterPosition;
	}

	public int bind(
			PreparedStatement statement,
			QueryParameters qp,
			SessionImplementor session,
			int position) throws SQLException {
		Object value = qp.getFilteredPositionalParameterValues()[queryParameterPosition];
		definedParameterType.nullSafeSet( statement, value, position, session );
		return definedParameterType.getColumnSpan( session.getFactory() );
	}

	public Type getExpectedType() {
		return definedParameterType;
	}

	public void setExpectedType(Type expectedType) {
		// todo : throw exception?
	}

	public String renderDisplayInfo() {
		return "dynamic-filter={filterName=" + filterName + ",paramName=" + parameterName + "}";
	}
}
