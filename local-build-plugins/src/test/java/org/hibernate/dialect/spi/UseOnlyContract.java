/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.spi;

import org.hibernate.SPI;

/// Non-implementable hierarchy fixture.
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public interface UseOnlyContract {
}
