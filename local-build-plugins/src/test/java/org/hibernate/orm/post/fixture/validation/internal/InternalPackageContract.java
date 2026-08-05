/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post.fixture.validation.internal;

import org.hibernate.SPI;

/// Negative fixture for an SPI declaration in an internal package.
///
/// @author Steve Ebersole
@SPI
public interface InternalPackageContract {
}
