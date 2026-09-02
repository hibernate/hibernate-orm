/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/**
 * Optional {@link Type} contract for implementations that are aware of
 * how to extract values from stored procedure OUT/INOUT parameters.
 *
 * @author Steve Ebersole
 */
@SPI({ USE, IMPLEMENT })
public interface ProcedureParameterExtractionAware<T>
		extends BindableType<T>, OutputableType<T> {
}
