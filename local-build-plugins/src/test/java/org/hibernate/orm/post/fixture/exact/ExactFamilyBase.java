/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post.fixture.exact;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Directly implementable family-base fixture.
///
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT })
public abstract class ExactFamilyBase {
}
