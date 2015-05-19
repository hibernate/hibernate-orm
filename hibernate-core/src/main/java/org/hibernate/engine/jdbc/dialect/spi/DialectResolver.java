/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.dialect.spi;

import org.hibernate.dialect.Dialect;
import org.hibernate.service.Service;

/**
 * Contract for determining the {@link Dialect} to use based on information about the database / driver.
 *
 * @author Tomoto Shimizu Washio
 * @author Steve Ebersole
 */
public interface DialectResolver extends Service {
	/**
	 * Determine the {@link Dialect} to use based on the given information.  Implementations are expected to return
	 * the {@link Dialect} instance to use, or {@code null} if the they did not locate a match.
	 *
	 * @param info Access to the information about the database/driver needed to perform the resolution
	 *
	 * @return The dialect to use, or null.
	 */
	public Dialect resolveDialect(DialectResolutionInfo info);
}
