/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.type;

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * Optional {@link Type} contract for implementations that are aware of how to extract values from
 * store procedure OUT/INOUT parameters.
 *
 * @author Steve Ebersole
 */
public interface ProcedureParameterExtractionAware<T> extends Type {
	/**
	 * Can the given instance of this type actually perform the parameter value extractions?
	 *
	 * @return {@code true} indicates that @{link #extract} calls will not fail due to {@link IllegalStateException}.
	 */
	public boolean canDoExtraction();

	/**
	 * Perform the extraction
	 *
	 * @param statement The CallableStatement from which to extract the parameter value(s).
	 * @param startIndex The parameter index from which to start extracting; assumes the values (if multiple) are contiguous
	 * @param session The originating session
	 *
	 * @return The extracted value.
	 *
	 * @throws SQLException Indicates an issue calling into the CallableStatement
	 * @throws IllegalStateException Thrown if this method is called on instances that return {@code false} for {@link #canDoExtraction}
	 */
	public T extract(CallableStatement statement, int startIndex, SessionImplementor session) throws SQLException;

	/**
	 * Perform the extraction
	 *
	 * @param statement The CallableStatement from which to extract the parameter value(s).
	 * @param paramNames The parameter names.
	 * @param session The originating session
	 *
	 * @return The extracted value.
	 *
	 * @throws SQLException Indicates an issue calling into the CallableStatement
	 * @throws IllegalStateException Thrown if this method is called on instances that return {@code false} for {@link #canDoExtraction}
	 */
	public T extract(CallableStatement statement, String[] paramNames, SessionImplementor session) throws SQLException;
}
