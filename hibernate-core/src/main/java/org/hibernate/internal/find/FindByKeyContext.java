/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.find;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/// Context for find by primary or natural key operations.
///
/// @author Steve Ebersole
public interface FindByKeyContext {
	SharedSessionContractImplementor getOrigin();
}
