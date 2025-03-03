/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

/**
 * The type of action from which the cache call is originating.
 */
public enum CachedNaturalIdValueSource {
	LOAD,
	INSERT,
	UPDATE
}
