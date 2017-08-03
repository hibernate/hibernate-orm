/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

/**
 * A collector for {@link Initializer} instances.
 *
 * @see QueryResult#registerInitializers
 * @see Fetch#registerInitializers
 *
 * @author Steve Ebersole
 */
public interface InitializerCollector {
	/**
	 * Collect the passed Initializer
	 */
	void addInitializer(Initializer initializer);
}
