/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

/**
 * Commonality for loading a {@linkplain Loadable loadable} in "batch" (more than one key at a time)
 *
 * @author Steve Ebersole
 */
public interface BatchLoader extends MultiKeyLoader {
	/**
	 * The total number of {@linkplain Loadable loadable} references that can be initialized per each load operation.
	 */
	int getDomainBatchSize();
}
