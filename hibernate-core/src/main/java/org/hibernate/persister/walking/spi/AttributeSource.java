/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.walking.spi;

/**
* @author Steve Ebersole
*/
public interface AttributeSource {
	default int getPropertyIndex(String propertyName) {
		throw new UnsupportedOperationException();
	}
}
