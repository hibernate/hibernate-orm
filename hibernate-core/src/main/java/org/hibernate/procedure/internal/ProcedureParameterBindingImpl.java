/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.procedure.spi.ProcedureParameterBindingImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.internal.QueryParameterBindingImpl;
import org.hibernate.query.procedure.ProcedureParameterBinding;

/**
 * Implementation of the {@link ProcedureParameterBinding} contract.
 *
 * @author Steve Ebersole
 */
public class ProcedureParameterBindingImpl<T>
		extends QueryParameterBindingImpl<T>
		implements ProcedureParameterBindingImplementor<T> {
	public ProcedureParameterBindingImpl(
			ProcedureParameterImplementor<T> queryParameter,
			SessionFactoryImplementor sessionFactory) {
		super( queryParameter, sessionFactory );
	}
}
