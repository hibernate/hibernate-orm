/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.jdbc.spi;

import java.sql.Connection;

/**
 * @author Steve Ebersole
 */
public interface JdbcObserver {
	public void jdbcConnectionAcquisitionStart();
	public void jdbcConnectionAcquisitionEnd(Connection connection);

	public void jdbcConnectionReleaseStart();
	public void jdbcConnectionReleaseEnd();

	public void jdbcPrepareStatementStart();
	public void jdbcPrepareStatementEnd();

	public void jdbcExecuteStatementStart();
	public void jdbcExecuteStatementEnd();

	public void jdbcExecuteBatchStart();
	public void jdbcExecuteBatchEnd();

}
