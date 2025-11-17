/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;

/**
 * Boot-time descriptor of a named procedure/function query, as defined in
 * annotations or xml
 *
 * @see jakarta.persistence.NamedStoredProcedureQuery
 *
 * @author Steve Ebersole
 */
public interface NamedProcedureCallDefinition extends NamedQueryDefinition<Object> {
	/**
	 * The name of the underlying database procedure or function name
	 */
	String getProcedureName();

	@Override
	default Class<Object> getResultType() {
		return Object.class;
	}

	@Override
	NamedCallableQueryMemento resolve(SessionFactoryImplementor factory);
}
