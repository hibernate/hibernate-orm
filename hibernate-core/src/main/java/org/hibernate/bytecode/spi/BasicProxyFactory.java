/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.spi;

/**
 * A proxy factory for "basic proxy" generation.
 *
 * @author Steve Ebersole
 */
public interface BasicProxyFactory {
	/**
	 * Get a proxy reference..
	 *
	 * @return A proxy reference.
	 */
	Object getProxy();
}
