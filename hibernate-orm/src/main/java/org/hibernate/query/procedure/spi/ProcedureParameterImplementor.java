/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.procedure.spi;

import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.query.procedure.ProcedureParameter;

/**
 * @author Steve Ebersole
 */
public interface ProcedureParameterImplementor<T> extends ProcedureParameter<T> {
	ParameterRegistrationImplementor<T> getNativeParameterRegistration();
}
