/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.jdbc.spi;

import java.sql.Connection;

/**
 * @deprecated It is no longer possible to plug custom implementations of
 * this SPI. It will be removed.
 * @author Steve Ebersole
 */
@Deprecated
public interface JdbcObserver {
	void jdbcConnectionAcquisitionStart();
	void jdbcConnectionAcquisitionEnd(Connection connection);

	void jdbcConnectionReleaseStart();
	void jdbcConnectionReleaseEnd();

	void jdbcPrepareStatementStart();
	void jdbcPrepareStatementEnd();

	void jdbcExecuteStatementStart();
	void jdbcExecuteStatementEnd();

	void jdbcExecuteBatchStart();
	void jdbcExecuteBatchEnd();

	default void jdbcReleaseRegistryResourcesStart() {}
	default void jdbcReleaseRegistryResourcesEnd() {}


}
