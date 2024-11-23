/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
