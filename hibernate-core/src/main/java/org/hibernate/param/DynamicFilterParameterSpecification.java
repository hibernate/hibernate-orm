/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.param;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.Type;

/**
 * A specialized ParameterSpecification impl for dealing with a dynamic filter parameters.
 * 
 * @see org.hibernate.Session#enableFilter(String)
 *
 * @author Steve Ebersole
 */
public class DynamicFilterParameterSpecification implements ParameterSpecification {
	private final String filterName;
	private final String parameterName;
	private final Type definedParameterType;

	/**
	 * Constructs a parameter specification for a particular filter parameter.
	 *
	 * @param filterName The name of the filter
	 * @param parameterName The name of the parameter
	 * @param definedParameterType The paremeter type specified on the filter metadata
	 */
	public DynamicFilterParameterSpecification(
			String filterName,
			String parameterName,
			Type definedParameterType) {
		this.filterName = filterName;
		this.parameterName = parameterName;
		this.definedParameterType = definedParameterType;
	}

	@Override
	public int bind(
			PreparedStatement statement,
			QueryParameters qp,
			SharedSessionContractImplementor session,
			int start) throws SQLException {
		final int columnSpan = definedParameterType.getColumnSpan( session.getFactory() );
		final Object value = session.getLoadQueryInfluencers().getFilterParameterValue( filterName + '.' + parameterName );
		if ( Collection.class.isInstance( value ) ) {
			int positions = 0;
			Iterator itr = ( ( Collection ) value ).iterator();
			while ( itr.hasNext() ) {
				definedParameterType.nullSafeSet( statement, itr.next(), start + positions, session );
				positions += columnSpan;
			}
			return positions;
		}
		else {
			definedParameterType.nullSafeSet( statement, value, start, session );
			return columnSpan;
		}
	}

	@Override
	public Type getExpectedType() {
		return definedParameterType;
	}

	@Override
	public void setExpectedType(Type expectedType) {
		// todo : throw exception?  maybe warn if not the same?
	}

	@Override
	public String renderDisplayInfo() {
		return "dynamic-filter={filterName=" + filterName + ",paramName=" + parameterName + "}";
	}
}
