/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.procedure.spi.ProcedureParameterBinding;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.internal.QueryParameterBindingImpl;

/**
 * Implementation of the {@link ProcedureParameterBinding} contract.
 *
 * @author Steve Ebersole
 */
public class ProcedureParameterBindingImpl<T>
		extends QueryParameterBindingImpl<T>
		implements ProcedureParameterBinding<T> {
	public ProcedureParameterBindingImpl(
			ProcedureParameterImplementor<T> queryParameter,
			SessionFactoryImplementor sessionFactory) {
		super( queryParameter, sessionFactory );
	}
}
