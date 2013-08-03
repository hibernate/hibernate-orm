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
package org.hibernate.procedure;

import org.hibernate.result.Outputs;

/**
 * Specialization of the {@link org.hibernate.result.Outputs} contract providing access to the stored procedure's registered
 * output parameters.
 *
 * @author Steve Ebersole
 */
public interface ProcedureOutputs extends Outputs {
	/**
	 * Retrieve the value of an OUTPUT parameter by the parameter's registration memento.
	 * <p/>
	 * Should NOT be called for parameters registered as REF_CURSOR.  REF_CURSOR parameters should be
	 * accessed via the returns (see {@link #getNextOutput}
	 *
	 * @param parameterRegistration The parameter's registration memento.
	 *
	 * @return The output value.
	 *
	 * @see ProcedureCall#registerParameter(String, Class, javax.persistence.ParameterMode)
	 */
	public <T> T getOutputParameterValue(ParameterRegistration<T> parameterRegistration);

	/**
	 * Retrieve the value of an OUTPUT parameter by the name under which the parameter was registered.
	 *
	 * @param name The name under which the parameter was registered.
	 *
	 * @return The output value.
	 *
	 * @throws ParameterStrategyException If the ProcedureCall is defined using positional parameters
	 * @throws NoSuchParameterException If no parameter with that name exists
	 *
	 * @see ProcedureCall#registerParameter(String, Class, javax.persistence.ParameterMode)
	 */
	public Object getOutputParameterValue(String name);

	/**
	 * Retrieve the value of an OUTPUT parameter by the name position under which the parameter was registered.
	 *
	 * @param position The position at which the parameter was registered.
	 *
	 * @return The output value.
	 *
	 * @throws ParameterStrategyException If the ProcedureCall is defined using named parameters
	 * @throws NoSuchParameterException If no parameter with that position exists
	 *
	 * @see ProcedureCall#registerParameter(int, Class, javax.persistence.ParameterMode)
	 */
	public Object getOutputParameterValue(int position);
}
