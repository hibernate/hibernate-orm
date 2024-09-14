/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
