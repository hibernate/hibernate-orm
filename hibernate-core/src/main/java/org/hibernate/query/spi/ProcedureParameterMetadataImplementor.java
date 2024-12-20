/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import java.util.List;

import org.hibernate.procedure.spi.ProcedureParameterImplementor;

public interface ProcedureParameterMetadataImplementor extends ParameterMetadataImplementor {

	List<? extends ProcedureParameterImplementor<?>> getRegistrationsAsList();
}
