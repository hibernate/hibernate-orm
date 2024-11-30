/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.procedure;

import org.hibernate.query.spi.QueryParameterBinding;

/**
 * Describes an input value binding for any IN/INOUT parameters.
 *
 * @author Steve Ebersole
 */
public interface ProcedureParameterBinding<T> extends QueryParameterBinding<T> {
}
