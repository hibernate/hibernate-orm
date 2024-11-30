/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.query.OutputableType;

/**
 * Optional {@link Type} contract for implementations that are aware of how to extract values from
 * store procedure OUT/INOUT parameters.
 *
 * @author Steve Ebersole
 */
public interface ProcedureParameterExtractionAware<T> extends OutputableType<T> {
}
