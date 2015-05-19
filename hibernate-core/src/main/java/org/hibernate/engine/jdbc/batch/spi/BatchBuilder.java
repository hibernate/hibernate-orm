/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.batch.spi;

import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.service.Service;
import org.hibernate.service.spi.Manageable;

/**
 * A builder for {@link Batch} instances
 *
 * @author Steve Ebersole
 */
public interface BatchBuilder extends Service, Manageable {
	/**
	 * Build a batch.
	 *
	 * @param key Value to uniquely identify a batch
	 * @param jdbcCoordinator The JDBC coordinator with which to coordinate efforts
	 *
	 * @return The built batch
	 */
	public Batch buildBatch(BatchKey key, JdbcCoordinator jdbcCoordinator);
}
