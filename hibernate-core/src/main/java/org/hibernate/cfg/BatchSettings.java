/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

import java.sql.PreparedStatement;

import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;

/**
 * @author Steve Ebersole
 */
public interface BatchSettings {
	/**
	 * Names the {@link BatchBuilder} implementation to use.
	 *
	 * @settingDefault Standard builder based on {@link #STATEMENT_BATCH_SIZE}
	 */
	String BUILDER = "hibernate.jdbc.batch.builder";

	/**
	 * Specifies the maximum number of {@linkplain java.sql.PreparedStatement statements}
	 * to {@linkplain PreparedStatement#addBatch batch} together in a stateful session.
	 * <p>
	 * Any positive value enables batching.
	 * <p>
	 * This setting has no effect on {@linkplain org.hibernate.StatelessSession stateless sessions}.
	 *
	 * @see java.sql.PreparedStatement#executeBatch
	 * @see java.sql.PreparedStatement#addBatch
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyJdbcBatchSize
	 *
	 * @settingDefault 0
	 */
	String STATEMENT_BATCH_SIZE = "hibernate.jdbc.batch_size";

	/**
	 * Enable ordering of update statements by primary key value, for the purpose of more
	 * efficient JDBC batching
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyOrderingOfUpdates
	 *
	 * @settingDefault {@code false}
	 */
	String ORDER_UPDATES = "hibernate.order_updates";

	/**
	 * Enable ordering of insert statements by primary key value, for the purpose of more
	 * efficient JDBC batching.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyOrderingOfInserts
	 *
	 * @settingDefault {@code false}
	 */
	String ORDER_INSERTS = "hibernate.order_inserts";

	/**
	 * @deprecated Use {@link #BUILDER} instead
	 */
	@Deprecated(since="6.4")
	String BATCH_STRATEGY = "hibernate.jdbc.factory_class";
}
