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
package org.hibernate;

/**
 * Represents all the outputs of a call to a database stored procedure (or function) through the JDBC
 * {@link java.sql.CallableStatement} interface.
 *
 * @author Steve Ebersole
 */
public interface StoredProcedureOutputs {
	/**
	 * Retrieve the value of an OUTPUT parameter by the name under which the parameter was registered.
	 *
	 * @param name The name under which the parameter was registered.
	 *
	 * @return The output value.
	 *
	 * @see StoredProcedureCall#registerStoredProcedureParameter(String, Class, javax.persistence.ParameterMode)
	 */
	public Object getOutputParameterValue(String name);

	/**
	 * Retrieve the value of an OUTPUT parameter by the name position under which the parameter was registered.
	 *
	 * @param position The position at which the parameter was registered.
	 *
	 * @return The output value.
	 *
	 * @see StoredProcedureCall#registerStoredProcedureParameter(int, Class, javax.persistence.ParameterMode)
	 */
	public Object getOutputParameterValue(int position);

	/**
	 * Are there any more returns associated with this set of outputs?
	 *
	 * @return {@code true} means there are more results available via {@link #getNextReturn()}; {@code false}
	 * indicates that calling {@link #getNextReturn()} will certainly result in an exception.
	 */
	public boolean hasMoreReturns();

	/**
	 * Retrieve the next return.
	 *
	 * @return The next return.
	 */
	public StoredProcedureReturn getNextReturn();
}
