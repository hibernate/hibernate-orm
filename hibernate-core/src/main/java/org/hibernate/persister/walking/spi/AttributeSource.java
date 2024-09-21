/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
