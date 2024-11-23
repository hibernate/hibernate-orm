/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.spi;

import org.hibernate.query.procedure.ProcedureParameterBinding;

/**
 * @author Steve Ebersole
 */
public interface ProcedureParameterBindingImplementor<T> extends ProcedureParameterBinding<T> {
}
