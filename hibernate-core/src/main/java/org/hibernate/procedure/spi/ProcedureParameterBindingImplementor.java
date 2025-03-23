/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.spi;

import org.hibernate.query.procedure.ProcedureParameterBinding;

/**
 * @author Steve Ebersole
 */
public interface ProcedureParameterBindingImplementor<T> extends ProcedureParameterBinding<T> {
}
