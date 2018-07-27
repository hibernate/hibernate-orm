/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Optional {@link Type} contract for implementations that are aware of how to extract values from
 * store procedure OUT/INOUT parameters.
 *
 * @author Steve Ebersole
 */
public interface ProcedureParameterExtractionAware<T> {
	/**
	 * Can the given instance of this type actually perform the parameter value extractions?
	 *
	 * @return {@code true} indicates that @{link #extract} calls will not fail due to {@link IllegalStateException}.
	 */
	boolean canDoExtraction();

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
	T extract(CallableStatement statement, int startIndex, SharedSessionContractImplementor session) throws SQLException;

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
	T extract(CallableStatement statement, String[] paramNames, SharedSessionContractImplementor session) throws SQLException;
}
