/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes source for attributes which can be fetched.
 *
 * @author Steve Ebersole
 */
public interface FetchableAttributeSource {
	FetchCharacteristics getFetchCharacteristics();
}
