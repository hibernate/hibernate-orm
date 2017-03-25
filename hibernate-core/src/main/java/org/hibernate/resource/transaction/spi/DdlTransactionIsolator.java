/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.spi;

import java.sql.Connection;

import org.hibernate.tool.schema.internal.exec.JdbcContext;

/**
 * Provides access to a Connection that is isolated from
 * any "current transaction" with the designed purpose of
 * performing DDL commands
 *
 * @author Steve Ebersole
 */
public interface DdlTransactionIsolator {
	JdbcContext getJdbcContext();

	/**
	 * In general a DdlTransactionIsolator should be returned from
	 * {@link TransactionCoordinatorBuilder#buildDdlTransactionIsolator}
	 * already prepared for use (until {@link #release} is called).
	 *
	 * @deprecated Instances should be returned from
	 * {@link TransactionCoordinatorBuilder#buildDdlTransactionIsolator}
	 * already prepared for use
	 */
	@Deprecated
	void prepare();

	/**
	 * Returns a Connection that is usable within the bounds of the
	 * {@link #prepare} and {@link #release} calls.  Further, this
	 * Connection will be isolated (transactionally) from any
	 * transaction in effect prior to the call to {@link #prepare}.
	 *
	 * @return
	 */
	Connection getIsolatedConnection();

	void release();
}
