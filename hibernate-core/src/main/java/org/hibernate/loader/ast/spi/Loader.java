/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

/**
 * Common contract for all value-mapping loaders.
 *
 * @author Steve Ebersole
 */
public interface Loader {
	/**
	 * The value-mapping loaded by this loader
	 */
	Loadable getLoadable();
}
