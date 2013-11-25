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

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.TypedValue;

/**
 * Parameter bind specification for an explicit named parameter.
 *
 * @author Steve Ebersole
 */
public class NamedParameterSpecification extends AbstractExplicitParameterSpecification {
	private final String name;

	/**
	 * Constructs a named parameter bind specification.
	 *
	 * @param sourceLine See {@link #getSourceLine()}
	 * @param sourceColumn See {@link #getSourceColumn()} 
	 * @param name The named parameter name.
	 */
	public NamedParameterSpecification(int sourceLine, int sourceColumn, String name) {
		super( sourceLine, sourceColumn );
		this.name = name;
	}

	/**
	 * Bind the appropriate value into the given statement at the specified position.
	 *
	 * @param statement The statement into which the value should be bound.
	 * @param qp The defined values for the current query execution.
	 * @param session The session against which the current execution is occuring.
	 * @param position The position from which to start binding value(s).
	 *
	 * @return The number of sql bind positions "eaten" by this bind operation.
	 */
	@Override
	public int bind(PreparedStatement statement, QueryParameters qp, SessionImplementor session, int position)
	        throws SQLException {
		TypedValue typedValue = qp.getNamedParameters().get( name );
		typedValue.getType().nullSafeSet( statement, typedValue.getValue(), position, session );
		return typedValue.getType().getColumnSpan( session.getFactory() );
	}

	@Override
	public String renderDisplayInfo() {
		return "name=" + name + ", expectedType=" + getExpectedType();
	}

	/**
	 * Getter for property 'name'.
	 *
	 * @return Value for property 'name'.
	 */
	public String getName() {
		return name;
	}
}
