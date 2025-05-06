/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Steve Ebersole
 */
public interface JaxbEmbeddable extends JaxbManagedType {
	@Nullable
	String getName();
	void setName(@Nullable String name);
}
