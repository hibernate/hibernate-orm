/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
