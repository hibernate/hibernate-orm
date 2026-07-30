/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

/**
 * The inheritance type for a given entity hierarchy
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
@Remove
public enum InheritanceType {
	NO_INHERITANCE,
	DISCRIMINATED,
	JOINED,
	UNION
}
