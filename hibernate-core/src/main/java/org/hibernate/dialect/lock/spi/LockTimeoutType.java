/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

/**
 * @author Steve Ebersole
 */
public enum LockTimeoutType {
	NONE,
	QUERY,
	CONNECTION
}
