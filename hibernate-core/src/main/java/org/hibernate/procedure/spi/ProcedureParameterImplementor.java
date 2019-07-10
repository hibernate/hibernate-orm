/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.procedure.spi;

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.Incubating;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.procedure.ProcedureParameter;
import org.hibernate.query.spi.QueryParameterImplementor;

/**
 * SPI extension for ProcedureParameter
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ProcedureParameterImplementor<T> extends ProcedureParameter<T>, QueryParameterImplementor<T> {
	/**
	 * Allow the parameter to register itself with the JDBC CallableStatement,
	 * if necessary, as well as perform any other needed preparation for exeuction
	 *
	 * @throws SQLException Indicates a problem with any underlying JDBC calls
	 */
	default void prepare(
			CallableStatement statement,
			int startIndex,
			ProcedureCallImplementor<?> callImplementor) throws SQLException{
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
