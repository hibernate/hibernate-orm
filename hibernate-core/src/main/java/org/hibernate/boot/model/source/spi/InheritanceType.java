/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * The inheritance type for a given entity hierarchy
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public enum InheritanceType {
	NO_INHERITANCE,
	DISCRIMINATED,
	JOINED,
	UNION
}
