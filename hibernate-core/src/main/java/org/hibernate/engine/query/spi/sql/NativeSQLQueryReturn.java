/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi.sql;


/**
 * Describes a return in a native SQL query.
 * <p/>
 * IMPL NOTE : implementations should be immutable as they are used as part of cache keys for result caching.
 *
 * @author Steve Ebersole
 */
public interface NativeSQLQueryReturn {
	public static interface TraceLogger {
		public void writeLine(String traceLine);
	}

	public void traceLog(TraceLogger logger);
}
