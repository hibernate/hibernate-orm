/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post.fixture.spi;

import org.hibernate.Internal;

/// Proves that an explicit internal annotation overrides the SPI package
/// convention.
///
/// @author Steve Ebersole
@Internal
public class InternalOverrideContract {
}
