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

import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Parameter bind specification for an explicit  positional (or ordinal) parameter.
 *
 * @author Steve Ebersole
 */
public class PositionalParameterSpecification extends AbstractExplicitParameterSpecification implements ParameterSpecification {
	private final int hqlPosition;

	/**
	 * Constructs a position/ordinal parameter bind specification.
	 *
	 * @param sourceLine See {@link #getSourceLine()}
	 * @param sourceColumn See {@link #getSourceColumn()}
	 * @param hqlPosition The position in the source query, relative to the other source positional parameters.
	 */
	public PositionalParameterSpecification(int sourceLine, int sourceColumn, int hqlPosition) {
		super( sourceLine, sourceColumn );
		this.hqlPosition = hqlPosition;
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
	public int bind(PreparedStatement statement, QueryParameters qp, SessionImplementor session, int position) throws SQLException {
		Type type = qp.getPositionalParameterTypes()[hqlPosition];
		Object value = qp.getPositionalParameterValues()[hqlPosition];

		type.nullSafeSet( statement, value, position, session );
		return type.getColumnSpan( session.getFactory() );
	}

	/**
	 * {@inheritDoc}
	 */
	public String renderDisplayInfo() {
		return "ordinal=" + hqlPosition + ", expectedType=" + getExpectedType();
	}

	/**
	 * Getter for property 'hqlPosition'.
	 *
	 * @return Value for property 'hqlPosition'.
	 */
	public int getHqlPosition() {
		return hqlPosition;
	}
}
