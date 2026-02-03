/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.discovery.spi;

import java.util.function.Consumer;

/// Coordinates discovery of managed classes.
///
/// @author Steve Ebersole
public interface Discovery {
	/// Perform discovery based on the specified `boundaries`
	/// passing all discovered classes to the specified `classNameConsumer`.
	void discoverClassNames(Boundaries boundaries, Consumer<String> classNameConsumer);
}
