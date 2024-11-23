/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
	 * to {@linkplain PreparedStatement#addBatch batch} together.
	 * <p/>
	 * A nonzero value enables batching
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
	 * When enabled, specifies that {@linkplain jakarta.persistence.Version versioned}
	 * data should be included in batching.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyJdbcBatchingForVersionedEntities(boolean)
	 *
	 * @settingDefault Generally {@code true}, though can vary based on Dialect
	 */
	String BATCH_VERSIONED_DATA = "hibernate.jdbc.batch_versioned_data";

	/**
	 * @deprecated Use {@link #BUILDER} instead
	 */
	@Deprecated(since="6.4")
	String BATCH_STRATEGY = "hibernate.jdbc.factory_class";
}
