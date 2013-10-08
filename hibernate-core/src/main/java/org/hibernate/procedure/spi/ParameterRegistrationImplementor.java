/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.procedure.spi;

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.procedure.ParameterRegistration;
import org.hibernate.type.Type;

/**
 * Additional internal contract for ParameterRegistration
 *
 * @author Steve Ebersole
 */
public interface ParameterRegistrationImplementor<T> extends ParameterRegistration<T> {
	/**
	 * Prepare for execution.
	 *
	 * @param statement The statement about to be executed
	 * @param i The parameter index for this registration (used for positional)
	 *
	 * @throws SQLException Indicates a problem accessing the statement object
	 */
	public void prepare(CallableStatement statement, int i) throws SQLException;

	/**
	 * Access to the Hibernate type for this parameter registration
	 *
	 * @return The Hibernate Type
	 */
	public Type getHibernateType();

	/**
	 * Access to the SQL type(s) for this parameter
	 *
	 * @return The SQL types (JDBC type codes)
	 */
	public int[] getSqlTypes();

	/**
	 * Extract value from the statement after execution (used for OUT/INOUT parameters).
	 *
	 * @param statement The callable statement
	 *
	 * @return The extracted value
	 */
	public T extract(CallableStatement statement);

}
