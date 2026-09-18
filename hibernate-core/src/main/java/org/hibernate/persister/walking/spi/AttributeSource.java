/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.walking.spi;

import jakarta.annotation.Nonnull;

/**
* @author Steve Ebersole
*/
public interface AttributeSource {
	default int getPropertyIndex(@Nonnull String propertyName) {
		throw new UnsupportedOperationException();
	}
}
