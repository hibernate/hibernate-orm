/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Specialization of {@link org.hibernate.metamodel.model.domain.DomainType} for types that
 * can be used as a parameter output for a {@link org.hibernate.procedure.ProcedureCall}.
 *
 * @apiNote We assume a type that maps to exactly one SQL value, hence {@link #getJdbcType()}
 *
 * @author Steve Ebersole
 */
@Incubating
public interface OutputableType<J> extends BindableType<J> {
	/**
	 * Can the given instance of this type actually perform the parameter value extractions?
	 *
	 * @return {@code true} indicates that {@link #extract} calls will not fail due to {@link IllegalStateException}.
	 */
	boolean canDoExtraction();

	/**
	 * Descriptor for the SQL type mapped by this type.
	 */
	JdbcType getJdbcType();

	/**
	 * Perform the extraction
	 *
	 * @param statement The CallableStatement from which to extract the parameter value(s).
	 * @param paramIndex The parameter index from which to extract
	 * @param session The originating session
	 *
	 * @return The extracted value.
	 *
	 * @throws SQLException Indicates an issue calling into the CallableStatement
	 * @throws IllegalStateException Thrown if this method is called on instances that return {@code false} for {@link #canDoExtraction}
	 */
	J extract(CallableStatement statement, int paramIndex, SharedSessionContractImplementor session) throws SQLException;

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
