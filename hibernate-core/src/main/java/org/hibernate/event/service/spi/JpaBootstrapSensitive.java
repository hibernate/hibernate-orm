/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.service.spi;

/**
 * Defines an event listener that is sensitive to whether a native or jpa bootstrap was performed
 *
 * @author Steve Ebersole
 *
 * @deprecated This is no longer implemented by any listener
 */
@Deprecated(since = "7")
public interface JpaBootstrapSensitive {
	void wasJpaBootstrap(boolean wasJpaBootstrap);
}
