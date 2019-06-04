/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * Specialization of DomainType for types that can be used as a
 * parameter output for a {@link org.hibernate.procedure.ProcedureCall}
 *
 * @apiNote We assume a type that maps to exactly one SQL value, hence
 * {@link #getSqlTypeDescriptor()}
 *
 * @author Steve Ebersole
 */
public interface AllowableOutputParameterType<J> extends AllowableParameterType<J> {
	/**
	 * Can the given instance of this type actually perform the parameter value extractions?
	 *
	 * @return {@code true} indicates that @{link #extract} calls will not fail due to {@link IllegalStateException}.
	 */
	boolean canDoExtraction();

	/**
	 * Descriptor for the SQL type mapped by this type.
	 */
	SqlTypeDescriptor getSqlTypeDescriptor();

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
	J extract(CallableStatement statement, int startIndex, SharedSessionContractImplementor session) throws SQLException;

	/**
	 * Perform the extraction
	 *
	 * @param statement The CallableStatement from which to extract the parameter value(s).
	 * @param paramName The parameter names.
	 * @param session The originating session
	 *
	 * @return The extracted value.
	 *
	 * @throws SQLException Indicates an issue calling into the CallableStatement
	 * @throws IllegalStateException Thrown if this method is called on instances that return {@code false} for {@link #canDoExtraction}
	 */
	J extract(CallableStatement statement, String paramName, SharedSessionContractImplementor session) throws SQLException;
}
