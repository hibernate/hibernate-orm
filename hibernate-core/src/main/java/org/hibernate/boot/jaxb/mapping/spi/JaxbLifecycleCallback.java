/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

/**
 * JAXB binding interface for lifecycle callbacks.
 *
 * @author Strong Liu
 * @author Steve Ebersole
 */
public interface JaxbLifecycleCallback {
	String getMethodName();
}
